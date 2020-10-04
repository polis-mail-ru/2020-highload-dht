package ru.mail.polis.dao.valaubr;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SSTable implements Table {

    private final FileChannel fileChannel;

    private final int size;
    private final int shift;

    SSTable(@NotNull final File file) throws IOException {
        fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final int fileSize = (int) fileChannel.size();

        final ByteBuffer offsetBuf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(offsetBuf, fileSize - Integer.BYTES);
        size = offsetBuf.flip().getInt();

        shift = fileSize - Integer.BYTES * (1 + size);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            int pos = getPosition(from.rewind());

            @Override
            public boolean hasNext() {
                return pos < size;
            }

            @Override
            public Cell next() {
                try {
                    return getCell(pos++);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    static void serialize(
            final File file,
            final Iterator<Cell> iterator) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (iterator.hasNext()) {
                final Cell cell = iterator.next();
                final ByteBuffer key = cell.getKey();
                final Value value = cell.getValue();
                final int keySize = key.remaining();
                offsets.add(offset);
                offset += keySize + Integer.BYTES * 2 + Long.BYTES;

                fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(keySize).flip());

                fileChannel.write(key);

                fileChannel.write(ByteBuffer.allocate(Long.BYTES).putLong(value.getTimestamp()).flip());

                if (value.isTombstone()) {
                    fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(-1).flip());
                } else {
                    final ByteBuffer valueBuffer = value.getData();
                    final int valueSize = valueBuffer.remaining();
                    fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(valueSize).flip());
                    fileChannel.write(valueBuffer);
                    offset += valueSize;
                }
            }

            for (final Integer i : offsets) {
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(i).flip());
            }
            fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(offsets.size()).flip());
        }
    }

    private int getOffset(final int position) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(buffer, shift + position * Integer.BYTES);
        return buffer.flip().getInt();
    }

    private ByteBuffer getKey(final int position) throws IOException {
        final int keyLengthOffset = getOffset(position);

        final ByteBuffer keySizeBuf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(keySizeBuf, keyLengthOffset);

        final ByteBuffer keyBuf = ByteBuffer.allocate(keySizeBuf.flip().getInt());
        fileChannel.read(keyBuf, keyLengthOffset + Integer.BYTES);

        return keyBuf.flip();
    }

    private int getPosition(final ByteBuffer key) throws IOException {
        int left = 0;
        int right = size - 1;
        while (left <= right) {
            final int mid = (left + right) / 2;
            final ByteBuffer midValue = getKey(mid);
            final int cmp = midValue.compareTo(key);

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

    private Cell getCell(final int position) throws IOException {

        int elementOffset = getOffset(position);

        final ByteBuffer key = getKey(position);

        elementOffset += Integer.BYTES + key.remaining();
        final ByteBuffer timestampBuf = ByteBuffer.allocate(Long.BYTES);
        fileChannel.read(timestampBuf, elementOffset);

        final ByteBuffer valueSizeBuf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(valueSizeBuf, elementOffset + Long.BYTES);
        final int valueSize = valueSizeBuf.flip().getInt();

        final Value value;
        if (valueSize == -1) {
            value = new Value(timestampBuf.flip().getLong());
        } else {
            final ByteBuffer valueBuf = ByteBuffer.allocate(valueSize);
            fileChannel.read(valueBuf, elementOffset + Long.BYTES + Integer.BYTES);
            valueBuf.flip();
            value = new Value(timestampBuf.flip().getLong(), valueBuf);
        }
        return new Cell(key, value);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        throw new UnsupportedOperationException("Sorry, operation upsert doesn`t exit, read-only table.");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("Sorry,operation remove doesn`t exit, read-only table.");
    }

    @Override
    public long getSizeInByte() {
        try {
            return fileChannel.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
