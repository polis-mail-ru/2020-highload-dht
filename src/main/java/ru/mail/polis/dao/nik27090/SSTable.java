package ru.mail.polis.dao.nik27090;

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

public class SSTable implements Table, Closeable {

    private final FileChannel channel;
    private final int amountElement;
    private final long indexStart;

    /**
     * Stores data in bit representation.
     * file structure: [row] ... [index] ... number of row.
     * row - keyLen, keyBytes, timeStamp, isAlive, valueLen, valueBytes.
     * index - start point position of row.
     *
     * @param file file created using serialize()
     */
    SSTable(@NotNull final File file) throws IOException {
        channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final long size = channel.size();
        channel.position(size - Integer.BYTES);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        channel.read(byteBuffer);
        this.amountElement = byteBuffer.rewind().getInt();
        this.indexStart = size - (amountElement + 1) * Integer.BYTES;
    }

    private int getOffset(final int numRow) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        channel.read(buf, indexStart + numRow * Integer.BYTES);
        return buf.rewind().getInt();
    }

    /**
     * Converts MemTable to SSTable and writes it on disk.
     *
     * @param file     temporary file for recording
     * @param iterator contains all Cell of MemTable
     */
    static void serialize(final File file, final Iterator<Cell> iterator) throws IOException {
        try (FileChannel fileChannel = new FileOutputStream(file).getChannel()) {
            final ArrayList<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (iterator.hasNext()) {
                offsets.add(offset);
                final Cell buf = iterator.next();
                final ByteBuffer key = buf.getKey();
                final Value value = buf.getValue();
                final int keySize = key.remaining();
                offset += Integer.BYTES + keySize + Long.BYTES;
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(keySize)
                        .rewind());
                fileChannel.write(key);
                if (value.isTombstone()) {
                    fileChannel.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(-value.getTimestamp())
                            .rewind());
                } else {
                    fileChannel.write(ByteBuffer.allocate(Long.BYTES)
                            .putLong(value.getTimestamp())
                            .rewind());
                    final ByteBuffer data = value.getContent();
                    final int valueSize = data.remaining();
                    offset += Integer.BYTES + valueSize;
                    fileChannel.write(ByteBuffer.allocate(Integer.BYTES)
                            .putInt(valueSize)
                            .rewind());
                    fileChannel.write(data);
                }
            }
            final int offsetSize = offsets.size();
            for (final Integer off : offsets) {
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(off)
                        .rewind());
            }
            fileChannel.write(ByteBuffer.allocate(Integer.BYTES)
                    .putInt(offsetSize)
                    .rewind());
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            int next = findElement(from);

            @Override
            public boolean hasNext() {
                return next < amountElement;
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

    private int findElement(@NotNull final ByteBuffer from) throws IOException {
        int l = 0;
        int r = amountElement - 1;
        while (l <= r) {
            final int med = (l + r) / 2;
            final int cmp = getKeyByOrder(med).compareTo(from);
            if (cmp < 0) {
                l = med + 1;
            } else if (cmp > 0) {
                r = med - 1;
            } else {
                return med;
            }
        }
        return l;
    }

    @NotNull
    private ByteBuffer getKeyByOrder(final int order) throws IOException {
        final int offset = getOffset(order);
        final ByteBuffer keySize = ByteBuffer.allocate(Integer.BYTES);
        channel.read(keySize, offset);
        final ByteBuffer key = ByteBuffer.allocate(keySize.rewind().getInt());
        channel.read(key, offset + Integer.BYTES);
        return key.rewind();
    }

    @NotNull
    private Cell getCell(final int row) throws IOException {
        int offset = getOffset(row);
        final ByteBuffer key = getKeyByOrder(row);
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

    /**
     * closes the channel.
     */
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
