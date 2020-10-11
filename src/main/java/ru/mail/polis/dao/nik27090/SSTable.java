package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SSTable implements Table, Closeable {

    private final FileChannel channel;
    private final long amountElement;

    private long iterPosition;
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
        this.channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        final long size = channel.size();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);

        channel.read(byteBuffer, size - Long.BYTES);
        this.amountElement = byteBuffer.getLong(0);

        this.indexStart = size - Long.BYTES - Long.BYTES * amountElement;
    }

    /**
     * Converts MemTable to SSTable and writes it on disk.
     *
     * @param file     temporary file for recording
     * @param iterator contains all Cell of MemTable
     */
    public static void serialize(final File file, final Iterator<Cell> iterator) {
        long size = 0;
        try (FileChannel fileChannel = FileChannel.open(file.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            final List<ByteBuffer> bufOffsetArray = new ArrayList<>();
            while (iterator.hasNext()) {
                size++;
                final Cell cell = iterator.next();
                //указатель
                bufOffsetArray.add(longToByteBuffer(fileChannel.position()));
                //keySize
                fileChannel.write(intToByteBuffer(cell.getKey().remaining()));
                //keyBytes
                fileChannel.write(cell.getKey());
                //isAlive
                if (cell.getValue().getContent() == null) {
                    //отрицательный timestamp = tombstone
                    fileChannel.write(longToByteBuffer(-cell.getValue().getTimestamp()));
                } else {
                    fileChannel.write(longToByteBuffer(cell.getValue().getTimestamp()));
                    fileChannel.write(intToByteBuffer(cell.getValue().getContent().remaining()));
                    fileChannel.write(cell.getValue().getContent());
                }
            }
            //количество элементов
            bufOffsetArray.add(longToByteBuffer(size));
            for (final ByteBuffer buff : bufOffsetArray) {
                fileChannel.write(buff);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return new Iterator<>() {
            final ByteBuffer key = from.duplicate();
            long next = findElement(key);

            @Override
            public boolean hasNext() {
                return amountElement > next;
            }

            @Override
            public Cell next() {
                assert hasNext();
                return getNext(next++);
            }
        };
    }

    private Cell getNext(final long next) {
        try {
            return getCell(getKeyByOrder(next));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long findElement(final ByteBuffer from) {
        final ByteBuffer key = from.rewind().duplicate();
        long low = 0;
        long high = amountElement - 1;
        long mid;
        while (low <= high) {
            mid = low + (high - low) / 2;
            final ByteBuffer midKey;
            try {
                midKey = getKeyByOrder(mid);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            final int compare = midKey.compareTo(key);
            if (compare < 0) {
                low = mid + 1;
            } else if (compare > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low;
    }

    private ByteBuffer getKeyByOrder(final long order) throws IOException {
        final ByteBuffer bbIndex = ByteBuffer.allocate(Long.BYTES);
        channel.read(bbIndex, indexStart + order * Long.BYTES);
        iterPosition = bbIndex.getLong(0);

        //Not duplicate
        final ByteBuffer bbKeySize = ByteBuffer.allocate(Integer.BYTES);
        channel.read(bbKeySize, iterPosition);
        iterPosition += bbKeySize.limit();

        final ByteBuffer bbKeyValue = ByteBuffer.allocate(bbKeySize.getInt(0));
        channel.read(bbKeyValue, iterPosition);
        iterPosition += bbKeyValue.limit();

        return bbKeyValue.rewind();
    }

    private Cell getCell(final @NotNull ByteBuffer key) throws IOException {
        final ByteBuffer bbTimeStamp = ByteBuffer.allocate(Long.BYTES);
        channel.read(bbTimeStamp, iterPosition);
        final long timestamp = bbTimeStamp.getLong(0);
        iterPosition += bbTimeStamp.limit();

        ByteBuffer bbValueContent;
        if (timestamp < 0) {
            return new Cell(key.rewind(), new Value(-timestamp));
        } else {
            final ByteBuffer bbValueSize = ByteBuffer.allocate(Integer.BYTES);
            channel.read(bbValueSize, iterPosition);
            iterPosition += bbValueSize.limit();

            bbValueContent = ByteBuffer.allocate(bbValueSize.getInt(0));
            channel.read(bbValueContent, iterPosition);
            iterPosition += bbValueContent.limit();

            return new Cell(key.rewind(), new Value(timestamp, bbValueContent.rewind()));
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

    private static ByteBuffer longToByteBuffer(final long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).rewind();
    }

    private static ByteBuffer intToByteBuffer(final int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).rewind();
    }
}
