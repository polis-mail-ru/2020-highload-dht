package ru.mail.polis.dao.zvladn7;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ThreadSafe
public class SSTable implements Table {

    private static final Logger logger = LoggerFactory.getLogger(SSTable.class);

    private final int shiftToOffsetsArray;
    private final int amountOfElements;
    private final FileChannel fileChannel;

    /**
     * File structure.
     * * [ rows ]
     * * [ rows offset ]
     * * amount of rows
     */
    SSTable(@NotNull final File file) throws IOException {
        fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final int fileSize = (int) fileChannel.size();

        //get amount
        final ByteBuffer offsetBuf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(offsetBuf, fileSize - Integer.BYTES);
        amountOfElements = offsetBuf.flip().getInt();

        shiftToOffsetsArray = fileSize - Integer.BYTES * (1 + amountOfElements);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new SSTableIter(from);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException("SSTable doesn't provide upsert operations!");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException("SSTable doesn't provide remove operations!");
    }

    @Override
    public int size() {
        return amountOfElements;
    }

    static void serialize(final File file, final Iterator<Cell> elementsIter) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {

            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;

            while (elementsIter.hasNext()) {
                final Cell cell = elementsIter.next();
                final ByteBuffer key = cell.getKey();
                final Value value = cell.getValue();
                final int keySize = key.remaining();

                offsets.add(offset);
                offset += keySize + Integer.BYTES * 2 + Long.BYTES;

                //write key size
                fileChannel.write(ByteBuffer.allocate(Integer.BYTES).putInt(keySize).flip());
                //write key
                fileChannel.write(key);
                //write timestamp
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
        fileChannel.read(buffer,shiftToOffsetsArray + position * Integer.BYTES);
        return buffer.flip().getInt();
    }

    private ByteBuffer getKey(final int position) throws IOException {
        final int keyLengthOffset = getOffset(position);

        final ByteBuffer keySizeBuf = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(keySizeBuf, keyLengthOffset);

        final ByteBuffer keyBuf = ByteBuffer.allocate(keySizeBuf.flip().getInt());
        fileChannel.read(keyBuf,keyLengthOffset + Integer.BYTES);

        return keyBuf.flip();
    }

    private int getElementPosition(final ByteBuffer key) throws IOException {
        int left = 0;
        int right = amountOfElements - 1;
        while (left <= right) {
            final int mid = (left + right) / 2;
            final ByteBuffer midKey = getKey(mid);
            final int compareResult = midKey.compareTo(key);

            if (compareResult < 0) {
                left = mid + 1;
            } else if (compareResult > 0) {
                right = mid - 1;
            } else {
                return mid;
            }
        }

        return left;
    }

    /**
     * Cell(a row of file) structure.
     * key size | key | timestamp | value size | value
     * if value size is 0 than value is absent
     */
    private Cell get(final int position) throws IOException {
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
            value = Value.newTombstoneValue(timestampBuf.flip().getLong());
        } else {
            final ByteBuffer valueBuf = ByteBuffer.allocate(valueSize);
            fileChannel.read(valueBuf, elementOffset + Long.BYTES + Integer.BYTES);
            valueBuf.flip();
            value = new Value(timestampBuf.flip().getLong(), valueBuf);
        }

        return new Cell(key, value);
    }

    class SSTableIter implements Iterator<Cell> {

        private int position;

        public SSTableIter(final ByteBuffer from) {
            try {
                position = getElementPosition(from.rewind());
            } catch (IOException e) {
                logger.info("SSTable's iterator cannot correctly get 'from' position", e);
            }
        }

        @Override
        public boolean hasNext() {
            return position < amountOfElements;
        }

        @Override
        public Cell next() {
            try {
                return get(position++);
            } catch (IOException e) {
                logger.info("SSTable's iterator cannot get a cell because it has no more elements", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            logger.warn("The error was happened when the file channel was closed", e);
        }
    }
}
