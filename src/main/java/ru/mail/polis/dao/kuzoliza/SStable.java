package ru.mail.polis.dao.kuzoliza;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;

public class SStable implements Table, Closeable {
    private final FileChannel channel;
    private final int rows;
    private final long indexStart;

    SStable(@NotNull final File file) throws IOException {
        channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final long size = channel.size();
        channel.position(size - Integer.BYTES);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        channel.read(byteBuffer);
        rows = byteBuffer.rewind().getInt();
        indexStart = size - (rows + 1) * Integer.BYTES;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            int next = binarySearch(from);
            @Override
            public boolean hasNext() {
                return rows > next;
            }

            @Override
            public Cell next() {
                try {
                    return getCell(next++);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NotNull
    private Cell getCell(final int row) throws IOException {
        int offset = getOffset(row);
        final ByteBuffer key = getKey(row);
        offset += key.remaining() + Integer.BYTES;

        final ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        channel.read(timestamp, offset);
        offset += Long.BYTES;

        if (timestamp.rewind().getLong() < 0) {
            return new Cell(key, new Value(-timestamp.rewind().getLong()));

        } else {
            final ByteBuffer valueSize = ByteBuffer.allocate(Integer.BYTES);
            channel.read(valueSize, offset);
            final ByteBuffer value = ByteBuffer.allocate(valueSize.rewind().getInt());
            offset += Integer.BYTES;
            channel.read(value, offset);
            return new Cell(key, new Value(timestamp.rewind().getLong(), value.rewind()));
        }
    }

    private int binarySearch(@NotNull final ByteBuffer from) throws IOException {
        int left = 0;
        int right = rows - 1;
        while (left <= right) {
            final int pivot = (left + right) / 2;
            final int cmp = getKey(pivot).compareTo(from);
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

    @NotNull
    private ByteBuffer getKey(final int row) throws IOException {
        final int offset = getOffset(row);
        final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
        channel.read(keySize, offset);
        final ByteBuffer key = ByteBuffer.allocate(keySize.rewind().getInt());
        channel.read(key, offset + Integer.BYTES);
        return key.rewind();
    }

    private int getOffset(final int numRow) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        channel.read(buffer, indexStart + numRow * Integer.BYTES);
        return buffer.rewind().getInt();
    }

    static void serialize(final File file, final Iterator<Cell> iterator) throws IOException {
        try (FileChannel fileChannel = new FileOutputStream(file).getChannel()) {
            final ArrayList<Integer> offsets = new ArrayList<>();
            int offset = 0;

            while (iterator.hasNext()) {
                offsets.add(offset);
                final Cell cell = iterator.next();
                final ByteBuffer key = cell.getKey();
                final Value value = cell.getValue();
                final int keySize = key.remaining();

                offset += Integer.BYTES + keySize + Long.BYTES;
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(keySize).rewind());
                fileChannel.write(key);

                if (value.isTombstone()) {
                    fileChannel.write(ByteBuffer.allocate(Long.BYTES).putLong(-value.getTimestamp()).rewind());

                } else {
                    fileChannel.write(ByteBuffer.allocate(Long.BYTES).putLong(value.getTimestamp()).rewind());
                    final ByteBuffer data = value.getData();
                    final int valueSize = data.remaining();

                    offset += Integer.BYTES + valueSize;
                    fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(valueSize).rewind());
                    fileChannel.write(data);
                }
            }
            final int offsetSize = offsets.size();
            for (final Integer off : offsets) {
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(off).rewind());
            }
            fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(offsetSize).rewind());
        }
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
