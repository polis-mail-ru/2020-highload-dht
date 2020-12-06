package ru.mail.polis.dao.impl.tables;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.models.Value;
import ru.mail.polis.utils.ByteUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@ThreadSafe
public final class SSTable implements Table {
    private final int rows;
    private final int count;
    private final FileChannel fileChannel;

    /**
     * Creates disk memory table.
     *
     * @param file that represents table
     * @throws IOException if cannot open or read channel
     */
    public SSTable(@NotNull final File file) throws IOException {
        fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final int fileSize = (int) fileChannel.size() - Integer.BYTES;
        final ByteBuffer cellCount = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(cellCount.duplicate(), fileSize);
        count = cellCount.duplicate().rewind().getInt();
        rows = fileSize - count * Integer.BYTES;
    }

    /**
     * Writes table to file.
     *
     * @param cells iterator to walk through cells
     * @param file that represents table
     * @throws IOException if cannot open or read channel
     */
    public static void write(@NotNull final Iterator<Cell> cells,
                             @NotNull final File file) throws IOException {
        try (FileChannel fc = new FileOutputStream(file).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (cells.hasNext()) {
                offset = write(fc, cells, offset, offsets);
            }
            for (final Integer anOffset : offsets) {
                fc.write(ByteUtils.fromInt(anOffset));
            }
            fc.write(ByteUtils.fromInt(offsets.size()));
        }
    }

    private static int write(@NotNull final FileChannel fc,
                             @NotNull final Iterator<Cell> cells,
                             final int offset,
                             @NotNull final List<Integer> offsets) throws IOException {
        int curOffset = offset;
        final Cell cell = cells.next();
        final ByteBuffer key = cell.getKey();
        offsets.add(curOffset);
        curOffset += key.duplicate().remaining() + Long.BYTES + Integer.BYTES;
        fc.write(ByteUtils.fromInt(key.duplicate().remaining()));
        fc.write(key);
        final Value value = cell.getValue();
        if (value.isTombstone()) {
            fc.write(ByteUtils.fromLong(-value.getTimestamp()));
        } else {
            fc.write(ByteUtils.fromLong(value.getTimestamp()));
            final ByteBuffer data = Objects.requireNonNull(value.getData()).duplicate();
            curOffset += data.remaining();
            fc.write(data);
            final Instant expire = value.getExpire();
            if (Instant.MAX.equals(expire)) {
                curOffset++;
                fc.write(ByteUtils.fromByte((byte) 0));
            } else {
                curOffset++;
                fc.write(ByteUtils.fromByte((byte) 1));
                curOffset += (Long.BYTES + Integer.BYTES);
                fc.write(ByteUtils.fromInstant(expire));
            }
        }
        return curOffset;
    }

    private ByteBuffer keyAt(final int i) throws IOException {
        final int offset = getOffset(i);
        final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(keySize.duplicate(), offset);
        final int key = keySize.duplicate().rewind().getInt();
        final ByteBuffer keyBuffer = ByteBuffer.allocate(key);
        fileChannel.read(keyBuffer.duplicate(), offset + Integer.BYTES);
        return keyBuffer.duplicate().rewind();
    }

    private Cell cellAt(final int i) {
        try {
            int offset = getOffset(i);
            final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
            fileChannel.read(keySize.duplicate(), offset);
            offset += Integer.BYTES;
            final int key = keySize.duplicate().rewind().getInt();
            final ByteBuffer keyBuffer = ByteBuffer.allocate(key);
            fileChannel.read(keyBuffer.duplicate(), offset);
            offset += key;
            final ByteBuffer timestampBuffer = ByteBuffer.allocate(Long.BYTES);
            fileChannel.read(timestampBuffer.duplicate(), offset);
            final long timestamp = timestampBuffer.duplicate().rewind().getLong();
            if (timestamp < 0) {
                return new Cell(keyBuffer.duplicate().rewind(), new Value(-timestamp));
            } else {
                offset += Long.BYTES;
                final int dataSize;
                if (i == count - 1) {
                    dataSize = rows - offset;
                } else {
                    dataSize = getOffset(i + 1) - offset;
                }
                final ByteBuffer data = ByteBuffer.allocate(dataSize);
                fileChannel.read(data.duplicate(), offset);
                final ByteBuffer isExpiredBuffer = ByteBuffer.allocate(1);
                fileChannel.read(isExpiredBuffer.duplicate(), offset);
                final byte isExpired = isExpiredBuffer.duplicate().rewind().get();
                if (isExpired == 0) {
                    return new Cell(keyBuffer.duplicate().rewind(),
                            new Value(timestamp,
                                    data.duplicate().rewind()));
                }
                offset++;
                final ByteBuffer expireSecondsBuffer = ByteBuffer.allocate(Long.BYTES);
                fileChannel.read(expireSecondsBuffer.duplicate(), offset);
                final long expireSeconds = expireSecondsBuffer.duplicate().rewind().getLong();
                if (expireSeconds < 0 || expireSeconds > Instant.MAX.getEpochSecond()) {
                    return new Cell(keyBuffer.duplicate().rewind(),
                            new Value(timestamp,
                                    data.duplicate().rewind()));
                }
                offset += Long.BYTES;
                final ByteBuffer expireNanosBuffer = ByteBuffer.allocate(Integer.BYTES);
                fileChannel.read(expireNanosBuffer.duplicate(), offset);
                final int expireNanos = expireSecondsBuffer.duplicate().rewind().getInt();
                return new Cell(keyBuffer.duplicate().rewind(),
                        new Value(timestamp,
                                data.duplicate().rewind(),
                                Instant.ofEpochSecond(expireSeconds, expireNanos)));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int getOffset(final int num) throws IOException {
        final ByteBuffer offsetBB = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(offsetBB.duplicate(), rows + (long) num * Integer.BYTES);
        return offsetBB.duplicate().rewind().getInt();
    }

    private int getPosition(@NotNull final ByteBuffer from) throws IOException {
        int left = 0;
        int right = count - 1;
        while (left <= right) {
            final int mid = left + (right - left) / 2;
            final int cmp = keyAt(mid).compareTo(from);
            if (cmp < 0) {
                left = mid + 1;
            } else if (cmp > 0) {
                right = mid - 1;
            } else {
                return mid;
            }
        }
        return left;
    }

    @Override
    public long sizeInBytes() {
        return (long) rows + (long) (count + 1) * Integer.BYTES;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            int next = getPosition(from);
            @Override
            public boolean hasNext() {
                return next < count;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return cellAt(next++);
            }
        };
    }

    /**
     * Closes table FileChannel.
     */
    @Override
    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       @NotNull final Instant expire) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
    }
}
