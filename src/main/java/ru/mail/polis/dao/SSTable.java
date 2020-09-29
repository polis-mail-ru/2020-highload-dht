package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SSTable implements Table {
    private final FileChannel fileChannel;
    private final int rows;
    private final long fileSize;

    /**
     * Creates SSTable from file.
     */
    public SSTable(@NotNull final File file) throws IOException {
        this.fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        this.fileSize = fileChannel.size();
        final ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(buf, this.fileSize - Integer.BYTES);
        this.rows = buf.rewind().getInt();
    }

    /**
     * saves SSTable to file.
     */
    public static void serialize(final File file, final Iterator<Cell> cellIterator) throws IOException {
        try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW)) {
            final List<Long> offsets = new ArrayList<>();
            while (cellIterator.hasNext()) {
                offsets.add(fc.position()); // here we put position(offset) for row which we are going to write

                final Cell cell = cellIterator.next();
                final ByteBuffer key = cell.getKey();
                final Value value = cell.getValue();

                // Now we will write a row, where row = keyLength, keyBytes, timestamp, [valueLength, valueBytes]
                // We don't write valueLength, valueBytes if timestamp is negative (it means tombstone)
                fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(key.remaining()).rewind()); // keyLength
                fc.write(key.rewind()); // keyBytes
                if (value.isTombstone()) {
                    fc.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(-1 * value.getTimestamp()).rewind()); // ts is negative
                } else {
                    fc.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(value.getTimestamp()).rewind()); // ts is positive
                    fc.write(ByteBuffer.allocate(Integer.BYTES)
                            .putInt(value.getData().remaining()).rewind()); // valueLength
                    fc.write(value.getData().rewind()); // valueBytes
                }
            }

            for (final Long offset : offsets) {
                fc.write(ByteBuffer.allocate(Long.BYTES).putLong(offset).rewind());
            }
            // at the end of the file we write the number of rows
            fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(offsets.size()).rewind());
        }
    }

    private long getOffset(final int row) throws IOException {
        final ByteBuffer offset = ByteBuffer.allocate(Long.BYTES);
        fileChannel.read(offset, this.fileSize - Integer.BYTES - Long.BYTES * (rows - row));
        return offset.rewind().getLong();
    }

    @NotNull
    private ByteBuffer keyAt(final int row) throws IOException {
        assert 0 <= row && row <= rows;
        final long offset = getOffset(row);

        final ByteBuffer keyLengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(keyLengthBuffer, offset);

        final ByteBuffer keyBuffer = ByteBuffer.allocate(keyLengthBuffer.rewind().getInt());
        fileChannel.read(keyBuffer, offset + Integer.BYTES);
        return keyBuffer.rewind();
    }

    @NotNull
    private Value valueAt(final int row) throws IOException {
        assert 0 <= row && row <= rows;
        final long offset = getOffset(row);

        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES); // for keyLength
        fileChannel.read(buffer, offset);
        final int keyLength = buffer.rewind().getInt();

        buffer = ByteBuffer.allocate(Long.BYTES); // for timestamp
        fileChannel.read(buffer, offset + Integer.BYTES + keyLength);
        final long timestamp = buffer.rewind().getLong();

        buffer = ByteBuffer.allocate(Integer.BYTES); // for valueLength
        fileChannel.read(buffer, offset + Integer.BYTES + keyLength + Long.BYTES);
        final int valueLength = buffer.rewind().getInt();

        buffer = ByteBuffer.allocate(valueLength);
        fileChannel.read(buffer, offset + Integer.BYTES + keyLength + Long.BYTES + Integer.BYTES);

        if (timestamp >= 0) {
            return new Value(timestamp, buffer.rewind());
        } else {
            return new Value(-timestamp, null);
        }
    }

    private int binarySearch(final ByteBuffer from) throws IOException {
        int left = 0;
        int right = rows - 1;
        while (left <= right) {
            final int mid = (right + left) / 2;
            final int cmp = from.compareTo(keyAt(mid));
            if (cmp < 0) {
                right = mid - 1;
            } else if (cmp > 0) {
                left = mid + 1;
            } else {
                return mid;
            }
        }
        return left;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            private int next = binarySearch(from);

            @Override
            public boolean hasNext() {
                return next < rows;
            }

            @Override
            public Cell next() {
                try {
                    return new Cell(keyAt(next), valueAt(next++));
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException("Immutable!");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("Immutable!");
    }

    @Override
    public long sizeInBytes() {
        return fileSize;
    }

    @Override
    public int size() {
        return rows;
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}