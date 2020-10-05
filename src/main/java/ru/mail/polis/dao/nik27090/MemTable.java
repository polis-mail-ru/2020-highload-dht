package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {
    private final SortedMap<ByteBuffer, Value> sortedMap;
    private long sizeInBytes;
    private int size;

    /**
     * Creates table of data in memory.
     */
    public MemTable() {
        this.sortedMap = new TreeMap<>();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return sortedMap.tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey(), entry.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value valueOfElement = new Value(System.currentTimeMillis(), value.duplicate());
        final Value prevValue = sortedMap.put(key.duplicate(), valueOfElement);
        if (prevValue == null) {
            sizeInBytes += sizeOfElement(key, valueOfElement);
            size++;
        } else {
            sizeInBytes += sizeOfElement(key, valueOfElement) - sizeOfElement(key, prevValue);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value valueOfElement = new Value(System.currentTimeMillis());
        final Value prevValue = sortedMap.put(key.duplicate(), valueOfElement);
        if (prevValue == null) {
            sizeInBytes += sizeOfElement(key, valueOfElement);
            size++;
        } else {
            sizeInBytes += sizeOfElement(key, valueOfElement) - sizeOfElement(key, prevValue);
        }
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    /**
     * approximate Cell size calculation with alive value.
     * value = 74 + size (headline - 16, link - 4, timestamp(long) - 8, tombStone(boolean) - 1,
     * content(ByteBuffer) - (headline - 16, link - 4, int - 4, boolean - 1, byte[] - (16 + 4 + size)))
     * +
     * key = 44 + size (ByteBuffer - (16 + 4 + 4 + 16 + 4 + size))
     * =
     * 118 + keySize + contentSize
     *
     * @param key   key of Cell
     * @param value value of Cell
     */
    private static long sizeOfElement(final ByteBuffer key, final Value value) {
        final ByteBuffer contentSize = value.getContent();
        if (contentSize != null) {
            return 118L + key.remaining() + value.getContent().remaining();
        }
        return 118L + key.remaining();
    }

    public int size() {
        return size;
    }
}
