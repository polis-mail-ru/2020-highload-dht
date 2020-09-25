package ru.mail.polis.dao.art241111;

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

public class SSTable implements Table {
    private final int count;
    private final int size;
    private final FileChannel fileChannel;

    SSTable(@NotNull final File file) throws IOException {
        fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);

        final int fileSize = (int) fileChannel.size() - Integer.BYTES;
        final ByteBuffer cellCount = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(cellCount, fileSize);

        this.count = cellCount.rewind().getInt();
        this.size = fileSize - count * Integer.BYTES;
    }

    static void write(final File fileTable, final Iterator<Cell> iter) throws IOException {
        try (FileChannel file = new FileOutputStream(fileTable).getChannel()) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (iter.hasNext()) {
                final Cell cell = iter.next();
                final ByteBuffer key = cell.getKey();

                offsets.add(offset);

                offset += key.remaining() + Long.BYTES + Integer.BYTES;

                file.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(key.remaining())
                        .rewind());

                file.write(key);

                if (cell.getValue().isTombstone()) {
                    file.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(-cell.getValue().getVersion())
                            .rewind());
                } else {
                    file.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(cell.getValue().getVersion())
                            .rewind());
                    final ByteBuffer data = cell.getValue().getData();
                    offset += data.remaining();
                    file.write(data);
                }
            }
            final int count = offsets.size();

            for (final Integer integer : offsets) {
                file.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(integer)
                        .rewind());
            }

            file.write(ByteBuffer.allocate(Integer.BYTES)
                    .putInt(count)
                    .rewind());
        }
    }

    private ByteBuffer readOffset(final int key,final int offset) throws IOException {
        final ByteBuffer keySize = ByteBuffer.allocate(key);
        fileChannel.read(keySize, offset);
        return keySize;
    }

    private Cell getCell(final int num) {
        try {
            int offset = getOffset(num);
            final ByteBuffer keySizeBB = readOffset(Long.BYTES, offset);

            offset += Integer.BYTES;
            final int keySize = keySizeBB.rewind().getInt();
            final ByteBuffer key = readOffset(keySizeBB.rewind().getInt(),offset);

            offset += keySize;
            final ByteBuffer versionBB = readOffset(Long.BYTES, offset);

            final long version = versionBB.rewind().getLong();

            if (version < 0) {
                return new Cell(key.rewind(), Value.tombstone(-version));
            } else {
                offset += Long.BYTES;
                final int dataSize;
                if (num == this.count - 1) {
                    dataSize = this.size - offset;
                } else {
                    dataSize = getOffset(num + 1) - offset;
                }
                final ByteBuffer data = readOffset(dataSize, offset);
                return new Cell(key.rewind(), new Value(data.rewind(), version));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ByteBuffer getKey(final int num) throws IOException {
        final int offset = getOffset(num);

        final ByteBuffer keySizeBB = readOffset(Integer.BYTES, offset);
        final ByteBuffer key = readOffset(keySizeBB.rewind().getInt(), offset + Integer.BYTES);

        return key.rewind();
    }

    private int getOffset(final int num) throws IOException {
        final ByteBuffer offsetBB = readOffset(Integer.BYTES, size + num * Integer.BYTES);
        return offsetBB.rewind().getInt();
    }

    private int getKeyPosition(final ByteBuffer key) {
        int low = 0;
        int high = count - 1;
        while (low <= high) {
            final int mid = low + (high - low) / 2;
            int cmp;
            try {
                cmp = getKey(mid).compareTo(key);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            int position = getKeyPosition(from);

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
    public long sizeInBytes() {
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
