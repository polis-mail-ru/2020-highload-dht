package ru.mail.polis.dao.impl.tables;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.models.Value;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        fileChannel.read(cellCount, fileSize);
        count = cellCount.rewind().getInt();
        rows = fileSize - count * Integer.BYTES;
    }

    /**
     * Writes table to file.
     *
     * @param cells iterator to walk through cells
     * @param file that represents table
     * @throws IOException if cannot open or read channel
     */
    public static void write(final Iterator<Cell> cells, final File file) throws IOException {
        try (FileChannel fc = new FileOutputStream(file).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (cells.hasNext()) {
                final Cell cell = cells.next();
                final ByteBuffer key = cell.getKey();
                offsets.add(offset);
                offset += key.remaining() + Long.BYTES + Integer.BYTES;
                fc.write(fromInt(key.remaining()));
                fc.write(key);

                final Value value = cell.getValue();
                if (value.isTombstone()) {
                    fc.write(fromLong(-cell.getValue().getTimestamp()));
                } else {
                    fc.write(fromLong(cell.getValue().getTimestamp()));
                    final ByteBuffer data = cell.getValue().getData();
                    offset += data.remaining();
                    fc.write(data);
                }
            }

            for (final Integer anOffset : offsets) {
                fc.write(fromInt(anOffset));
            }

            fc.write(fromInt(offsets.size()));
        }
    }

    private ByteBuffer keyAt(final int i) throws IOException {
        final int offset = getOffset(i);
        final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(keySize, offset);
        final int key = keySize.rewind().getInt();
        final ByteBuffer keyBuffer = ByteBuffer.allocate(key);
        fileChannel.read(keyBuffer, offset + Integer.BYTES);
        return keyBuffer.rewind();
    }

    private Cell cellAt(final int i) {
        try {
            int offset = getOffset(i);
            final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
            fileChannel.read(keySize, offset);
            offset += Integer.BYTES;
            final int key = keySize.rewind().getInt();
            final ByteBuffer keyBuffer = ByteBuffer.allocate(key);
            fileChannel.read(keyBuffer, offset);
            offset += key;
            final ByteBuffer timestampBuffer = ByteBuffer.allocate(Long.BYTES);
            fileChannel.read(timestampBuffer, offset);
            final long timestamp = timestampBuffer.rewind().getLong();
            if (timestamp < 0) {
                return new Cell(keyBuffer.rewind(), new Value(-timestamp));
            } else {
                offset += Long.BYTES;
                final int dataSize;
                if (i == count - 1) {
                    dataSize = rows - offset;
                } else {
                    dataSize = getOffset(i + 1) - offset;
                }
                final ByteBuffer data = ByteBuffer.allocate(dataSize);
                fileChannel.read(data, offset);
                return new Cell(keyBuffer.rewind(), new Value(timestamp, data.rewind()));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int getOffset(final int num) throws IOException {
        final ByteBuffer offsetBB = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(offsetBB, rows + num * Integer.BYTES);
        return offsetBB.rewind().getInt();
    }

    private int getPosition(final ByteBuffer from) throws IOException {
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
        return (long) rows + (count + 1) * Integer.BYTES;
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

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
    }

    private static ByteBuffer fromInt(final int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).rewind();
    }

    private static ByteBuffer fromLong(final long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).rewind();
    }
}
