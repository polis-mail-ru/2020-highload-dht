package ru.mail.polis.dao.stasmilke;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

final class MemTable implements Table {
    private final SortedMap<ByteBuffer, Value> sortedMap = new TreeMap<>();
    private long sizeInBytes;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return sortedMap.tailMap(from)
                .entrySet()
                .stream()
                .map(x -> new Cell(x.getKey(), x.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final Value oldValue = sortedMap.get(key);
        final Value newValue = new Value(value, System.currentTimeMillis());
        if (oldValue == null) {
            sizeInBytes += key.remaining();
        } else {
            sizeInBytes -= oldValue.sizeInBytes();
        }
        sizeInBytes += newValue.sizeInBytes();
        sortedMap.put(key, newValue);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value oldValue = sortedMap.get(key);
        final Value newValue = new Value(System.currentTimeMillis());
        if (oldValue != null) {
            sizeInBytes -= oldValue.sizeInBytes();
        }
        sizeInBytes += newValue.sizeInBytes();
        sortedMap.put(key, newValue);
    }

    @Override
    public int size() {
        return sortedMap.size();
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() throws IOException {
        //Do nothing
    }
}
