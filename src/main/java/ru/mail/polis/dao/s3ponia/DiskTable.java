package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.logging.Logger;

public class DiskTable implements Closeable, Table {
    private static final Logger logger = Logger.getLogger(DiskTable.class.getName());
    private final int[] shifts;
    private final int generation;
    private final FileChannel fileChannel;

    private final Path filePath;

    public Path getFilePath() {
        return filePath;
    }
    
    private class DiskTableIterator implements Iterator<Table.ICell> {
        private int elementIndex;
        
        private LazyCell getLazyCell(final int index) {
            if (index >= shifts.length - 1) {
                throw new ArrayIndexOutOfBoundsException("Out of bound");
            }
            return readLazyCell(getElementShift(index), getElementSize(index));
        }
        
        private int getElementIndex(@NotNull final ByteBuffer key) {
            int left = 0;
            int right = shifts.length - 2;
            while (left <= right) {
                final int mid = (left + right) / 2;
                final ByteBuffer midKey = getLazyCell(mid).getKey();
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
        
        DiskTableIterator() {
            elementIndex = 0;
        }
        
        DiskTableIterator(@NotNull final ByteBuffer key) {
            elementIndex = getElementIndex(key);
        }
        
        @Override
        public boolean hasNext() {
            return elementIndex < shifts.length - 1;
        }
        
        @Override
        public LazyCell next() {
            final var result = getLazyCell(elementIndex);
            ++elementIndex;
            return result;
        }
    }
    
    private class LazyCell implements Table.ICell {
        final long position;
        final int size;
        static final int CACHE_SIZE = 100;
        ByteBuffer keyCache;
        Table.Value valueCache;
        
        public LazyCell(final long position, final int size) {
            this.position = position;
            this.size = size;
        }
        
        @Override
        @NotNull
        public ByteBuffer getKey() {
            if (keyCache != null) {
                return keyCache;
            }
            try {
                final var buffer = ByteBuffer.allocate(Integer.BYTES);
                assert fileChannel != null;
                fileChannel.read(buffer, position + Long.BYTES);
                final var keySize = buffer.flip().getInt();
                final var key = ByteBuffer.allocate(keySize);
                fileChannel.read(key, position + Long.BYTES + Integer.BYTES);
                if (keySize < CACHE_SIZE) {
                    keyCache = key.flip();
                    return keyCache;
                }
                return key.flip();
            } catch (IOException e) {
                logger.warning(e.toString());
                return ByteBuffer.allocate(0);
            }
        }
        
        @Override
        @NotNull
        public Table.Value getValue() {
            if (valueCache != null) {
                return valueCache;
            }
            try {
                final var valueSizeBuf = ByteBuffer.allocate(Long.BYTES);
                assert fileChannel != null;
                fileChannel.read(valueSizeBuf, position);
                final var deadFlagTimeStamp = valueSizeBuf.flip().getLong();
                final var buffer = ByteBuffer.allocate(Integer.BYTES);
                fileChannel.read(buffer, position + Long.BYTES);
                final var keySize = buffer.flip().getInt();
                final var valueBuf = ByteBuffer.allocate(size - Integer.BYTES - Long.BYTES - keySize);
                fileChannel.read(valueBuf, position + Long.BYTES + Integer.BYTES + keySize);
                
                final var value = Table.Value.of(valueBuf.flip(), deadFlagTimeStamp, generation);
                if (valueBuf.limit() < CACHE_SIZE) {
                    valueCache = value;
                }
                return value;
            } catch (IOException e) {
                logger.warning(e.toString());
                return Table.Value.of(ByteBuffer.allocate(0), -1);
            }
        }
        
        @Override
        public int compareTo(@NotNull final Table.ICell o) {
            return Comparator.comparing(Table.ICell::getKey).thenComparing(Table.ICell::getValue).compare(this, o);
        }
    }
    
    private int getElementSize(final int index) {
        if (index == shifts.length - 1) {
            return getShiftsArrayShift() - getElementShift(index);
        } else {
            return getElementShift(index + 1) - getElementShift(index);
        }
    }
    
    private int getShiftsArrayShift() {
        return shifts[shifts.length - 1];
    }
    
    private int getElementShift(final int index) {
        return shifts[index];
    }
    
    private LazyCell readLazyCell(final long position, final int size) {
        return new LazyCell(position, size);
    }
    
    /**
     * DiskTable default constructor.
     */
    public DiskTable() {
        shifts = null;
        filePath = null;
        fileChannel = null;
        generation = 0;
    }
    
    DiskTable(final Path path) throws IOException {
        fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        filePath = path;
        final var fileName = path.getFileName().toString();
        generation = Integer.parseInt(fileName.substring(0, fileName.length() - 3));
        final long size = fileChannel.size();
        final var buffSize = ByteBuffer.allocate(Integer.BYTES);
        fileChannel.read(buffSize, size - Integer.BYTES);
        final var elementsQuantity = buffSize.flip().getInt();
        final var arrayShift = (int) size - Integer.BYTES * (elementsQuantity + 1);
        shifts = new int[elementsQuantity + 1];
        final var buff = ByteBuffer.allocate(Integer.BYTES * shifts.length);
        fileChannel.read(buff, arrayShift);
        buff.flip().asIntBuffer().get(shifts);
        shifts[elementsQuantity] = arrayShift;
    }

    @Override
    public int getGeneration() {
        return generation;
    }

    @Override
    public int size() {
        return shifts.length;
    }

    @Override
    public Iterator<Table.ICell> iterator() {
        return new DiskTableIterator();
    }

    @Override
    public Iterator<Table.ICell> iterator(@NotNull final ByteBuffer from) {
        return new DiskTableIterator(from);
    }

    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
    }

    static DiskTable of(final Path path) {
        try {
            return new DiskTable(path);
        } catch (IOException e) {
            logger.warning(e.toString());
            return new DiskTable();
        }
    }
    
    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
