package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

final class SSTable implements Table {

    private static final Logger log = LoggerFactory.getLogger(SSTable.class);

    private final int byteSize;
    private final int count;
    private final FileChannel fileChannel;

    private static final ByteBuffer allocateIntBuffer = ByteBuffer.allocate(Integer.BYTES);
    private static final ByteBuffer allocateLongBuffer = ByteBuffer.allocate(Long.BYTES);

    SSTable(@NotNull final Path path) {
        try {
            this.fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            final int fileSize = (int) (fileChannel.size() - Integer.BYTES);
            final ByteBuffer cellByteBuffer = allocateIntBuffer.rewind();
            this.fileChannel.read(cellByteBuffer.rewind(), fileSize);
            this.count = cellByteBuffer.rewind().getInt();
            this.byteSize = fileSize - count * Integer.BYTES;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new UncheckedIOException(e);

        }
    }

    static void serialize(@NotNull final File toFile,
                          @NotNull final Iterator<Cell> iterator) throws IOException {
        try (FileChannel file = new FileOutputStream(toFile).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (iterator.hasNext()) {
                final Cell cell = iterator.next();
                final ByteBuffer key = cell.getKey();
                offsets.add(offset);
                offset += key.remaining() + Long.BYTES + Integer.BYTES;
                file.write(allocateIntBuffer.rewind().putInt(key.remaining()).rewind());

                file.write(key);
                if (cell.getValue().isTombstone()) {
                    file.write(allocateLongBuffer.rewind()
                            .putLong(-cell.getValue().getTimestamp())
                            .rewind());
                } else {
                    file.write(allocateLongBuffer.rewind()
                            .putLong(cell.getValue().getTimestamp())
                            .rewind());

                    final ByteBuffer data = cell.getValue().getData().duplicate();
                    offset += data.remaining();
                    file.write(data);
                }
            }
            offsets.forEach(offsetValue -> {
                try {
                    file.write(allocateIntBuffer.rewind()
                            .putInt(offsetValue)
                            .rewind());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            final int count = offsets.size();
            file.write(allocateIntBuffer.rewind().putInt(count).rewind());
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
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
    public long sizeInBytes() {
        return (long) byteSize + (count + 1) * Integer.BYTES;
    }

    private Cell getCell(final int rowPosition) {
        try {
            int offset = getOffset(rowPosition);
            final ByteBuffer keyByteBufferSize = allocateIntBuffer.rewind();
            fileChannel.read(keyByteBufferSize.duplicate(), offset);
            offset += Integer.BYTES;

            final int keySize = keyByteBufferSize.duplicate().rewind().getInt();
            final ByteBuffer keyByteBuffer = ByteBuffer.allocate(keySize);
            fileChannel.read(keyByteBuffer, offset);
            offset += keySize;

            final ByteBuffer timestampBuffer = allocateLongBuffer.rewind();
            fileChannel.read(timestampBuffer.duplicate(), offset);
            final long timestamp = timestampBuffer.duplicate().rewind().getLong();

            if (timestamp < 0) {
                return new Cell(keyByteBuffer.rewind(), new Value(-timestamp));
            } else {
                offset += Long.BYTES;
                final int dataSize;
                if (rowPosition == count - 1) {
                    dataSize = byteSize - offset;
                } else {
                    dataSize = getOffset(rowPosition + 1) - offset;
                }
                final ByteBuffer data = ByteBuffer.allocate(dataSize);
                fileChannel.read(data, offset);
                return new Cell(keyByteBuffer.rewind(), new Value(data.rewind(), timestamp));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int getKeyPosition(@NotNull final ByteBuffer from) throws IOException {
        assert count > 0;

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
        final ByteBuffer byteBuffer = allocateIntBuffer.rewind();
        final int offset = getOffset(rowPosition);

        fileChannel.read(byteBuffer.duplicate(), offset);
        final int size = byteBuffer.duplicate().rewind().getInt();
        final ByteBuffer keyBuffer = ByteBuffer.allocate(size);
        fileChannel.read(keyBuffer.duplicate(), offset + Integer.BYTES);
        return keyBuffer.rewind();
    }

    private int getOffset(final int rowPosition) throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(byteBuffer.duplicate(), byteSize + rowPosition * Integer.BYTES);
        return byteBuffer.rewind().getInt();
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
