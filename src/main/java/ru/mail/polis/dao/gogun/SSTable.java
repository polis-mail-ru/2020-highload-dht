package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SSTable implements Table {
    @NotNull
    private final FileChannel fc;
    private final int numOffsets;
    private final int dataSize;

    public SSTable(@NotNull final File file) throws IOException {
        this.fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final int fileSize = (int) fc.size();
        final ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        fc.read(buf, fileSize - Integer.BYTES);
        this.numOffsets = buf.flip().getInt();
        this.dataSize = fileSize - Integer.BYTES * (1 + numOffsets);
    }

    @NotNull
    private ByteBuffer key(final int row) throws IOException {
        //get pos of row
        final ByteBuffer startPos = ByteBuffer.allocate(Integer.BYTES);
        fc.read(startPos, dataSize + row * Integer.BYTES);
        final ByteBuffer keyLength = ByteBuffer.allocate(Integer.BYTES);
        //read key length
        int offset = startPos.flip().getInt();
        fc.read(keyLength, offset);
        offset += Integer.BYTES;
        //read key
        final ByteBuffer key = ByteBuffer.allocate(keyLength.flip().getInt());
        fc.read(key, offset);

        return key.flip();
    }

    @NotNull
    private Row row(final int row) throws IOException {
        int offset;
        final ByteBuffer key = key(row);
        final ByteBuffer startPos = ByteBuffer.allocate(Integer.BYTES);
        fc.read(startPos, dataSize + row * Integer.BYTES);
        //read timestamp
        final ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        offset = startPos.flip().getInt() + key.remaining() + Integer.BYTES;
        fc.read(timestamp, offset);
        offset += Long.BYTES;
        //read isAlive
        final ByteBuffer valueLength = ByteBuffer.allocate(Integer.BYTES);
        fc.read(valueLength, offset);
        offset += Integer.BYTES;
        if (valueLength.flip().getInt() != -1) {
            final ByteBuffer valueBytes = ByteBuffer.allocate(valueLength.flip().getInt());
            fc.read(valueBytes, offset);

            return new Row(key, new Value(timestamp.flip().getLong(), valueBytes.flip()));
        }

        return new Row(key, new Value(timestamp.flip().getLong()));
    }

    private int binarySearch(@NotNull final ByteBuffer from) throws IOException {
        int l = 0;
        int r = numOffsets - 1;
        while (l <= r) {
            final int mid = (l + r) / 2;
            final int cmp = key(mid).compareTo(from);

            if (cmp < 0) {
                l = mid + 1;
            } else if (cmp > 0) {
                r = mid - 1;
            } else {
                return mid;
            }
        }

        return l;
    }

    @NotNull
    @Override
    public Iterator<Row> iterator(@NotNull final ByteBuffer from) throws IOException {
        return new Iterator<>() {
            int pos = binarySearch(from.rewind());

            @Override
            public boolean hasNext() {
                return pos < numOffsets;
            }

            @Override
            public Row next() {
                try {
                    return row(pos++);
                } catch (IOException e) {
                    return null;
                }
            }
        };
    }

    public static void serialize(@NotNull final File file, final Iterator<Row> iterator) throws IOException {
        final List<Integer> offsets = new ArrayList<>();
        int offset = 0;
        try (FileChannel fc = new FileOutputStream(file).getChannel()) {
            while (iterator.hasNext()) {

                final Row row = iterator.next();
                final ByteBuffer key = row.getKey();
                final Value value = row.getValue();
                final int keySize = key.remaining();

                offsets.add(offset);
                offset += keySize + Integer.BYTES * 2 + Long.BYTES;

                //keyLength
                fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(keySize).flip());
                //keyBytes
                fc.write(key);
                //timestamp
                fc.write(ByteBuffer.allocate(Long.BYTES).putLong(value.getTimestamp()).flip());

                if (value.isTombstone()) {
                    fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(-1).flip());
                } else {
                    final ByteBuffer data = value.getData();
                    final int valueLength = data.remaining();
                    //valueLength
                    fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(valueLength).flip());
                    //valueBytes
                    fc.write(data);
                    offset += valueLength;
                }
            }

            for (final Integer i : offsets) {
                fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(i).flip());
            }

            fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(offsets.size()).flip());
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException("Immutable");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("Immutable");
    }

    @Override
    public void close() throws IOException {
        fc.close();
    }
}
