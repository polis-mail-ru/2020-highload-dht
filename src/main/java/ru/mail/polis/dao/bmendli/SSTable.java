package ru.mail.polis.dao.bmendli;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SSTable implements Table {

    private final FileChannel fileChannel;
    private final int size;
    private final int count;

    /**
     * Creates SSTable from passed file.
     *
     * @param path - file in which store data
     */
    public SSTable(@NotNull final Path path) throws IOException {
        this.fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        final int fileSize = (int) (fileChannel.size() - Integer.BYTES);
        final ByteBuffer cellByteBuffer = ByteBuffer.allocate(Integer.BYTES);
        this.fileChannel.read(cellByteBuffer, fileSize);
        this.count = cellByteBuffer.rewind().getInt();
        this.size = fileSize - count * Integer.BYTES;
    }

    /**
     * Saves data to file.
     *
     * @param toFile   - file destination
     * @param iterator - data
     */
    public static void serialize(@NotNull final File toFile,
                                 @NotNull final Iterator<Cell> iterator) throws IOException {
        try (FileChannel file = new FileOutputStream(toFile).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (iterator.hasNext()) {
                final Cell cell = iterator.next();
                final ByteBuffer key = cell.getKey().duplicate();
                offsets.add(offset);
                offset += key.remaining() + Long.BYTES + Integer.BYTES;
                file.write(ByteBuffer.allocate(Integer.BYTES).putInt(key.remaining()).rewind());

                file.write(key);
                if (cell.getValue().isTombstone()) {
                    if (!cell.getValue().isExpiredTombstone()) {
                        file.write(ByteBuffer.allocate(Long.BYTES)
                                .putLong(-cell.getValue().getTimestamp())
                                .rewind());
                    }
                } else if (!cell.getValue().isExpired()) {
                    file.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(cell.getValue().getTimestamp())
                            .rewind());
                    file.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(cell.getValue().getExpireTime())
                            .rewind());
                    ByteBuffer data = cell.getValue().getData();
                    if (data != null) {
                        data = data.duplicate();
                    }
                    offset += data.remaining() + Long.BYTES;
                    file.write(data);
                }
            }

            for (final int offsetValue : offsets) {
                file.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(offsetValue)
                        .rewind());
            }

            final int count = offsets.size();
            file.write(ByteBuffer.allocate(Integer.BYTES).putInt(count).rewind());
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value, final long expireTime) {
        throw new UnsupportedOperationException("upsert() not supporting for SSTable");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("remove() not supporting for SSTable");
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {

            int rowPosition = getKeyPosition(from);

            @Override
            public boolean hasNext() {
                return rowPosition < count;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return getCell(rowPosition++);
            }
        };
    }

    @Override
    public long size() {
        return (long) size + (count + 1) * Integer.BYTES;
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }

    private Cell getCell(final int rowPosition) {
        try {
            int offset = getOffset(rowPosition);
            final ByteBuffer keyByteBufferSize = ByteBuffer.allocate(Integer.BYTES);
            fileChannel.read(keyByteBufferSize, offset);
            offset += Integer.BYTES;

            final int keySize = keyByteBufferSize.rewind().getInt();
            final ByteBuffer keyByteBuffer = ByteBuffer.allocate(keySize);
            fileChannel.read(keyByteBuffer, offset);
            offset += keySize;

            final ByteBuffer timestampBuffer = ByteBuffer.allocate(Long.BYTES);
            fileChannel.read(timestampBuffer, offset);
            final long timestamp = timestampBuffer.rewind().getLong();
            offset += Long.BYTES;

            final ByteBuffer expireTimeBuffer = ByteBuffer.allocate(Long.BYTES);
            fileChannel.read(expireTimeBuffer, offset);
            final long expireTime = expireTimeBuffer.rewind().getLong();

            final boolean isExpire = expireTime != Value.NO_EXPIRATION
                    && timestamp + expireTime < System.currentTimeMillis();
            if (timestamp < 0 || isExpire) {
                return new Cell(keyByteBuffer.rewind(), Value.newInstance(-timestamp, Value.NO_EXPIRATION));
            } else {
                offset += Long.BYTES;
                final int dataSize;
                if (rowPosition == count - 1) {
                    dataSize = size - offset;
                } else {
                    dataSize = getOffset(rowPosition + 1) - offset;
                }
                final ByteBuffer data = ByteBuffer.allocate(dataSize);
                fileChannel.read(data, offset);
                return new Cell(keyByteBuffer.rewind(), Value.newInstance(data.rewind(), timestamp, expireTime));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int getKeyPosition(@NotNull final ByteBuffer from) throws IOException {
        assert count >= 0;

        int left = 0;
        int right = count - 1;

        while (left <= right) {
            final int mid = (left + right) / 2;
            final ByteBuffer keyByteBuffer = getKey(mid);
            final int resultCmp = keyByteBuffer.compareTo(from);
            if (resultCmp > 0) {
                right = mid - 1;
            } else if (resultCmp < 0) {
                left = mid + 1;
            } else {
                return mid;
            }
        }
        return left;
    }

    private ByteBuffer getKey(final int rowPosition) throws IOException {
        final ByteBuffer keyByteBuffer = ByteBuffer.allocate(Integer.BYTES);
        final int offset = getOffset(rowPosition);

        fileChannel.read(keyByteBuffer, offset);
        final int keySize = keyByteBuffer.rewind().getInt();
        final ByteBuffer keyBuffer = ByteBuffer.allocate(keySize);
        fileChannel.read(keyBuffer, offset + Integer.BYTES);
        return keyBuffer.rewind();
    }

    private int getOffset(final int rowPosition) throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(byteBuffer, size + rowPosition * Integer.BYTES);
        return byteBuffer.rewind().getInt();
    }
}
