package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

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

public final class SSTable implements Table {
    private final FileChannel fileChannel;
    private final int count;
    private final int size;

    public SSTable(@NotNull final File file) throws IOException {
        this.fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final int fileSize = (int) fileChannel.size() - Integer.BYTES;
        final ByteBuffer cellCount = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(cellCount, fileSize);
        this.count = cellCount.rewind().getInt();
        this.size = fileSize - count * Integer.BYTES;
    }

    /**
     * Метод записи таблицы в файл.
     *
     * @param fileTable    - директория
     * @param cellIterator - итератор
     * @throws IOException - I/O exception
     */
    public static void write(final File fileTable, final Iterator<Cell> cellIterator) throws IOException {
        try (FileChannel channel = new FileOutputStream(fileTable).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;

            while (cellIterator.hasNext()) {
                final Cell cell = cellIterator.next();
                final ByteBuffer key = cell.getKey();

                offsets.add(offset);
                offset += key.remaining() + Long.BYTES + Integer.BYTES;

                channel.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(key.remaining())
                        .rewind());
                channel.write(key);

                if (cell.getValue().isRemoved()) {
                    channel.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(-cell.getValue().getTimeStamp())
                            .rewind());
                } else {
                    channel.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(cell.getValue().getTimeStamp())
                            .rewind());

                    final ByteBuffer data = cell.getValue().getData();
                    offset += data.remaining();
                    channel.write(data);
                }
            }
            final int count = offsets.size();

            for (final Integer integer : offsets) {
                channel.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(integer)
                        .rewind());
            }
            channel.write(ByteBuffer.allocate(Integer.BYTES)
                    .putInt(count)
                    .rewind());
        }
    }

    private Cell getCell(final int num) {
        try {
            int offset = getOffset(num);
            final ByteBuffer keySizeByteBuffer = ByteBuffer.allocate(Integer.BYTES);
            fileChannel.read(keySizeByteBuffer, offset);
            offset += Integer.BYTES;

            final int keySize = keySizeByteBuffer.rewind().getInt();
            final ByteBuffer key = ByteBuffer.allocate(keySize);
            fileChannel.read(key, offset);
            offset += keySize;

            final ByteBuffer timestampByteBuffer = ByteBuffer.allocate(Long.BYTES);
            fileChannel.read(timestampByteBuffer, offset);
            final long timestamp = timestampByteBuffer.rewind().getLong();

            if (timestamp < 0) {
                return new Cell(key.rewind(), new Value(-timestamp));
            } else {
                offset += Long.BYTES;
                final int dataSize;

                if (num == this.count - 1) {
                    dataSize = this.size - offset;
                } else {
                    dataSize = getOffset(num + 1) - offset;
                }

                final ByteBuffer dataByteBuffer = ByteBuffer.allocate(dataSize);
                fileChannel.read(dataByteBuffer, offset);
                return new Cell(key.rewind(), new Value(timestamp, dataByteBuffer.rewind()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ByteBuffer getKey(final int num) throws IOException {
        final ByteBuffer keySizeByteBuffer = ByteBuffer.allocate(Integer.BYTES);
        final int offset = getOffset(num);
        fileChannel.read(keySizeByteBuffer, offset);
        final int keySize = keySizeByteBuffer.rewind().getInt();
        final ByteBuffer key = ByteBuffer.allocate(keySize);
        fileChannel.read(key, offset + Integer.BYTES);
        return key.rewind();
    }

    private int getOffset(final int num) throws IOException {
        final ByteBuffer offsetByteBuffer = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(offsetByteBuffer, size + num * Integer.BYTES);
        return offsetByteBuffer.rewind().getInt();
    }

    private int getPosition(final ByteBuffer key) {
        int left = 0;
        int right = count - 1;
        while (left <= right) {
            final int middle = left + (right - left) / 2;
            try {
                final int cmp = getKey(middle).compareTo(key);
                if (cmp < 0) {
                    left = middle + 1;
                } else if (cmp > 0) {
                    right = middle - 1;
                } else {
                    return middle;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return left;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            int position = getPosition(from);

            @Override
            public boolean hasNext() {
                return position < count;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return getCell(position++);
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

    @Override
    public long size() {
        return (long) size + (count + 1) * Integer.BYTES;
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
