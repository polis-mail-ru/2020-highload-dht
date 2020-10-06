package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SSTable implements Table {
    @NotNull
    private final FileChannel fc;
    private final File file;
    private final int numOffsets;
    private final long dataSize;

    public SSTable(@NotNull final File file) throws IOException {
        this.file = file;
        this.fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final long fileSize = fc.size();
        final ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        fc.read(buf, fileSize - Integer.BYTES);
        this.numOffsets = buf.rewind().getInt();
        this.dataSize = fileSize - Integer.BYTES - numOffsets * Long.BYTES;
    }

    @NotNull
    private ByteBuffer key(final int row) throws IOException {
        //get pos of row
        final ByteBuffer startPos = ByteBuffer.allocate(Long.BYTES);
        fc.read(startPos, dataSize + row * Long.BYTES);
        final ByteBuffer keyLength = ByteBuffer.allocate(Integer.BYTES);
        //read key length
        long offset = startPos.rewind().getLong();
        fc.read(keyLength, offset);
        offset += Integer.BYTES;
        //read key
        final ByteBuffer key = ByteBuffer.allocate(keyLength.rewind().getInt());
        fc.read(key, offset);
        
        return key.rewind();
    }

    @NotNull
    private Row row(final int row) throws IOException {
        long offset;
        final ByteBuffer key = key(row);
        final ByteBuffer startPos = ByteBuffer.allocate(Long.BYTES);
        fc.read(startPos, dataSize + row * Long.BYTES);
        //read timestamp
        final ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        offset = startPos.rewind().getLong() + key.remaining() + Integer.BYTES;
        fc.read(timestamp, offset);
        //read isAlive
        offset += Long.BYTES;
        final ByteBuffer isAlive = ByteBuffer.allocate(Byte.BYTES);
        fc.read(isAlive, offset);
        offset += Byte.BYTES;
        if (isAlive.rewind().get() == (byte) 1) {
            final ByteBuffer valueLength = ByteBuffer.allocate(Integer.BYTES);
            fc.read(valueLength, offset);
            offset += Integer.BYTES;
            final ByteBuffer valueBytes = ByteBuffer.allocate(valueLength.rewind().getInt());
            fc.read(valueBytes, offset);

            return new Row(key, new Value(timestamp.rewind().getLong(), valueBytes.rewind()));
        }

        return new Row(key, new Value(timestamp.rewind().getLong()));
    }

    @NotNull
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
            int pos = binarySearch(from);

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
        final List<ByteBuffer> offsets = new ArrayList<>();
        try (FileChannel fc = new FileOutputStream(file).getChannel()) {
            while (iterator.hasNext()) {
                offsets.add(ByteBuffer.allocate(Long.BYTES).putLong(fc.position()).rewind());

                final Row row = iterator.next();
                final ByteBuffer key = row.getKey();
                final Value value = row.getValue();

                //keyLength
                fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(key.remaining()).rewind());
                //keyBytes
                fc.write(key);
                //timestamp
                fc.write(ByteBuffer.allocate(Long.BYTES).putLong(value.getTimestamp()).rewind());

                if (value.isTombstone()) {
                    //isAlive
                    fc.write(ByteBuffer.allocate(Byte.BYTES).put((byte) 0).rewind());
                } else {
                    final ByteBuffer data = value.getData();
                    //isAlive
                    fc.write(ByteBuffer.allocate(Byte.BYTES).put((byte) 1).rewind());
                    //valueLength
                    fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(data.remaining()).rewind());
                    //valueBytes
                    fc.write(data);
                }
            }

            for (final ByteBuffer offset : offsets) {
                fc.write(offset);
            }

            fc.write(ByteBuffer.allocate(Integer.BYTES).putInt(offsets.size()).rewind());
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
