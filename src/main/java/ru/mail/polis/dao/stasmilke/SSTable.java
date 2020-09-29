package ru.mail.polis.dao.stasmilke;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

final class SSTable implements Table {

    private static final String HEADER_TEMP = "header.tmp";
    @NotNull
    private final FileChannel channel;
    private final int size;

    private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
    private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

    SSTable(@NotNull final File file) throws IOException {
        channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        channel.read(intBuffer.rewind(), channel.size() - Integer.BYTES);
        size = intBuffer.rewind().getInt();
    }

    private long offsetForRow(final int row) throws IOException {
        if (row == 0) {
            return 0;
        }

        channel.read(longBuffer.rewind(), channel.size() - Integer.BYTES + Long.BYTES * (-size + row));
        return longBuffer.rewind().getLong();
    }

    private ByteBuffer key(final long begin) throws IOException {
        long offset = begin;
        channel.read(intBuffer.rewind(), offset);

        final ByteBuffer key = ByteBuffer.allocate(intBuffer.rewind().getInt());
        offset += Integer.BYTES;
        channel.read(key, offset);
        return key.rewind();
    }

    private Cell cell(final long begin) throws IOException {
        long offset = begin;
        final ByteBuffer key = key(offset);
        offset += key.remaining() + Integer.BYTES;
        channel.read(longBuffer.rewind(), offset);
        final long timestamp = longBuffer.rewind().getLong();
        offset += Long.BYTES;
        if (timestamp < 0) {
            return new Cell(key, new Value(-timestamp));
        }

        channel.read(intBuffer.rewind(), offset);
        final ByteBuffer value = ByteBuffer.allocate(intBuffer.rewind().getInt());
        offset += Integer.BYTES;
        channel.read(value, offset);
        return new Cell(key, new Value(value.rewind(), timestamp));
    }

    private int binarySearch(@NotNull final ByteBuffer from) throws IOException {
        int left = 0;
        int right = size - 1;

        while (left <= right) {
            final int mid = (left + right) / 2;
            final int comp = from.compareTo(key(offsetForRow(mid)));
            if (comp < 0) {
                right = mid - 1;
            } else if (comp > 0) {
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
            private int nextRow = binarySearch(from);

            @Override
            public boolean hasNext() {
                return nextRow < size;
            }

            @Override
            public Cell next() {
                try {
                    return cell(offsetForRow(nextRow++));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public long sizeInBytes() throws IOException {
        return channel.size();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    static void serialize(
            @NotNull final File file,
            final Iterator<Cell> iterator
    ) throws IOException {
        try (FileChannel headerChannel = FileChannel.open(
                new File(file.getParentFile().toString() + HEADER_TEMP).toPath(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
                StandardOpenOption.DELETE_ON_CLOSE)) {
            try (FileChannel writeChannel = FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                int current = 0;
                long currentSize = 0;
                final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
                final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
                while (iterator.hasNext()) {
                    final Cell cell = iterator.next();

                    final int keySize = cell.getKey().remaining();
                    writeChannel.write(intBuffer.rewind().putInt(keySize).rewind(), currentSize);
                    currentSize += Integer.BYTES;
                    writeChannel.write(cell.getKey(), currentSize);
                    currentSize += keySize;

                    final long timestamp = cell.getValue().getTimestamp() * (cell.getValue().isTombstone() ? -1 : 1);
                    writeChannel.write(longBuffer.rewind().putLong(timestamp).rewind(), currentSize);
                    currentSize += Long.BYTES;

                    if (!cell.getValue().isTombstone()) {
                        final int valueSize = cell.getValue().getData().remaining();
                        writeChannel.write(intBuffer.rewind().putInt(valueSize).rewind(), currentSize);
                        currentSize += Integer.BYTES;
                        writeChannel.write(cell.getValue().getData(), currentSize);
                        currentSize += valueSize;
                    }
                    headerChannel.write(longBuffer.rewind().putLong(currentSize).rewind(), Long.BYTES * current);
                    current++;
                }
                writeChannel.transferFrom(headerChannel, currentSize, Long.BYTES * (current - 1));
                currentSize += Long.BYTES * (current - 1);
                writeChannel.write(intBuffer.rewind().putInt(current).rewind(), currentSize);
            }
        }
    }
}
