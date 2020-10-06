package ru.mail.polis.dao.kuzoliza;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SStable implements Table, Closeable {
    private final FileChannel channel;
    private final long rows;
    private long iterPosition;
    private final long indexStart;

    SStable(@NotNull final File file) throws IOException {
        // index : rows x long
        this.channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final long size = channel.size();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        channel.read(byteBuffer, size - Long.BYTES);
        this.rows = byteBuffer.getLong(0);
        this.indexStart = size - Long.BYTES - Long.BYTES * rows;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            long next = binarySearch(from);
            @Override
            public boolean hasNext() {
                return rows > next;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return getNextCell(next++);
            }
        };
    }

    private Cell getNextCell(final long next) {
        try {
            return getCell(getKeyByOrder(next));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long binarySearch(final ByteBuffer from) {
        final ByteBuffer key = from.rewind().duplicate();
        long left = 0;
        long right = rows - 1;
        long pivot;
        while (left <= right) {
            pivot = left + (right - left) / 2;
            final ByteBuffer pivotKey;
            try {
                pivotKey = getKeyByOrder(pivot);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            final long cmp = pivotKey.compareTo(key);
            if (cmp < 0) {
                left = pivot + 1;
            } else if (cmp > 0) {
                right = pivot - 1;
            } else {
                return pivot;
            }
        }
        return left;
    }

    private ByteBuffer getKeyByOrder(final long order) throws IOException {

        final ByteBuffer index = ByteBuffer.allocate(Long.BYTES);
        channel.read(index, indexStart + order * Long.BYTES);
        iterPosition = index.getLong(0);

        final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
        channel.read(keySize, iterPosition);
        iterPosition += keySize.limit();

        final ByteBuffer keyData = ByteBuffer.allocate(keySize.getInt(0));
        channel.read(keyData, iterPosition);
        iterPosition += keyData.limit();

        return keyData.rewind();
    }

    private Cell getCell(final @NotNull ByteBuffer key) throws IOException {

        final ByteBuffer bbTimestamp = ByteBuffer.allocate(Long.BYTES);
        channel.read(bbTimestamp, iterPosition);
        final long timestamp = bbTimestamp.getLong(0);
        iterPosition += bbTimestamp.limit();

        ByteBuffer valueData;
        if (timestamp < 0) {
            return new Cell(key.rewind(), new Value(-timestamp));
        } else {
            final ByteBuffer valueSize = ByteBuffer.allocate(Integer.BYTES);
            channel.read(valueSize, iterPosition);
            iterPosition += valueSize.limit();

            valueData = ByteBuffer.allocate(valueSize.getInt(0));
            channel.read(valueData, iterPosition);
            iterPosition += valueData.limit();

            return new Cell(key.rewind(), new Value(timestamp, valueData.rewind()));
        }
    }

    static void serialize(final File file, final Iterator<Cell> iterator) throws IOException {
        long size = 0;
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {

            final List<ByteBuffer> offsetArray = new ArrayList<>();
            while (iterator.hasNext()) {
                size++;
                final Cell cell = iterator.next();

                // pointer, keySize, key, timestamp, valueSize, value
                offsetArray.add(putLong(fileChannel.position()));
                fileChannel.write(putInt(cell.getKey().remaining()));
                fileChannel.write(cell.getKey());

                // timestamp < 0  =>  tombstone
                if (cell.getValue().getData() == null) {
                    fileChannel.write(putLong(-cell.getValue().getTimestamp()));
                } else {
                    fileChannel.write(putLong(cell.getValue().getTimestamp()));
                    fileChannel.write(putInt(cell.getValue().getData().remaining()));
                    fileChannel.write(cell.getValue().getData());
                }
            }

            offsetArray.add(putLong(size));
            for (final ByteBuffer buffer : offsetArray) {
                fileChannel.write(buffer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ByteBuffer putLong(final long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).rewind();
    }

    private static ByteBuffer putInt(final int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).rewind();
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException("Unsupported method!");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("Unsupported method!");
    }

}
