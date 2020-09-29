package ru.mail.polis.dao.dmrnz;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SSTable implements Table, Closeable {
    private final ByteBuffer cells;
    private final LongBuffer offsets;
    private final int rows;
    private final long sizeInBytes;
    private final FileChannel channel;
    private final File file;

    /**
     * Creates a new SSTable representation of data file.
     *
     * @param file to get a table
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    SSTable(@NotNull final File file) throws IOException {
        this.file = file;
        final long fileSize = file.length();
        assert fileSize != 0 && fileSize <= Integer.MAX_VALUE;
        this.sizeInBytes = file.length();

        final ByteBuffer mapped;
        try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            assert file.length() <= Integer.MAX_VALUE;
            this.channel = fc;
            mapped = fc.map(FileChannel.MapMode.READ_ONLY, 0L, fileSize).order(ByteOrder.BIG_ENDIAN);
        }

        final long rowsLong = mapped.getLong((int) (fileSize - Long.BYTES));
        assert rowsLong <= Integer.MAX_VALUE;
        this.rows = (int) rowsLong;

        final ByteBuffer offsetBuffer = mapped.duplicate();
        offsetBuffer.position(mapped.limit() - Long.BYTES * this.rows - Long.BYTES);
        offsetBuffer.limit(mapped.limit() - Long.BYTES);
        this.offsets = offsetBuffer.slice().asLongBuffer();

        final ByteBuffer cellBuffer = mapped.duplicate();
        cellBuffer.limit(offsetBuffer.position());
        this.cells = cellBuffer.slice();
    }

    /**
     * Writes {@link MemTable} to disk.
     *
     * @param cells is iterator of {@link MemTable}
     * @param to    is the path where data will be written
     * @throws IOException if an I/O error thrown by a visitor method
     */
    static void write(@NotNull final Iterator<Cell> cells, @NotNull final File to) throws IOException {
        try (FileChannel fc = FileChannel.open(
                to.toPath(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)
        ) {
            final List<Long> offsets = new ArrayList<>();
            long offset = 0;
            while (cells.hasNext()) {
                offsets.add(offset);

                final Cell cell = cells.next();

                offset += writeBuffer(fc, cell.getKey());

                final Value value = cell.getValue();

                if (value.isRemoved()) {
                    fc.write(Bytes.fromLong(-cell.getValue().getTimeStamp()));
                } else {
                    fc.write(Bytes.fromLong(cell.getValue().getTimeStamp()));
                }
                offset += Long.BYTES;

                if (!value.isRemoved()) {
                    offset += writeBuffer(fc, value.getData());
                }
            }

            for (final Long anOffset : offsets) {
                fc.write(Bytes.fromLong(anOffset));
            }

            fc.write(Bytes.fromLong(offsets.size()));
        }
    }

    private static long writeBuffer(@NotNull final FileChannel fc,
                                    @NotNull final ByteBuffer buffer) throws IOException {
        int offset = Integer.BYTES;
        final int valueSize = buffer.remaining();

        fc.write(Bytes.fromInt(valueSize));
        fc.write(buffer);

        offset += valueSize;

        return offset;
    }

    private ByteBuffer keyAt(final int i) {
        assert 0 <= i && i <= rows;

        final long offset = offsets.get(i);
        assert offset <= Integer.MAX_VALUE;
        final ByteBuffer key = cells.duplicate();

        key.position((int) (offset + Integer.BYTES));
        key.limit(key.position() + cells.getInt((int) offset));

        return key.slice();
    }

    private Cell cellAt(final int i) {
        assert 0 <= i && i < rows;
        long offset = offsets.get(i);
        assert offset <= Integer.MAX_VALUE;

        final int keySize = cells.getInt((int) offset);
        offset += Integer.BYTES;
        final ByteBuffer key = keyAt(i);
        offset += keySize;

        final long timestamp = cells.getLong((int) offset);
        offset += Long.BYTES;

        if (timestamp < 0) {
            return new Cell(key.slice(), new Value(-timestamp, null));
        } else {
            final int valueSize = cells.getInt((int) offset);
            offset += Integer.BYTES;
            final ByteBuffer value = cells.duplicate();

            value.position((int) offset);
            value.limit(value.position() + valueSize);

            return new Cell(key.slice(), new Value(timestamp, value.slice()));
        }
    }

    private int position(@NotNull final ByteBuffer from, @NotNull final Boolean isDirect) {
        int left = 0;
        int right = rows - 1;

        while (left <= right) {
            final int mid = left + (right - left) / 2;
            final int cmp = from.compareTo(keyAt(mid));

            if (cmp < 0) {
                right = mid - 1;
            } else if (cmp > 0) {
                left = mid + 1;
            } else {
                return mid;
            }
        }
        return isDirect ? left : right;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            int next = position(from, true);

            @Override
            public boolean hasNext() {
                return next < rows;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return cellAt(next++);
            }
        };
    }

    @NotNull
    @Override
    public Iterator<Cell> reverseIterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            int next = position(from, false);

            @Override
            public boolean hasNext() {
                return next >= 0;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return cellAt(next--);
            }
        };
    }

    @NotNull
    @Override
    public Iterator<Cell> reverseIterator() {
        return new Iterator<>() {
            int last = rows - 1;

            @Override
            public boolean hasNext() {
                return last >= 0;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return cellAt(last--);
            }
        };
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException("SSTable is immutable");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("SSTable is immutable");
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public void deleteSSTableFile() throws IOException {
        Files.delete(file.toPath());
    }
}
