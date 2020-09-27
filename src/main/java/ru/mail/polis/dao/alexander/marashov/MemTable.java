package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {

    private final SortedMap<ByteBuffer, Value> map;
    private long sizeInBytes;

    public MemTable() {
        map = new TreeMap<>();
        sizeInBytes = 0;
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map((entry) -> new Cell(entry.getKey(), entry.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        sizeInBytes += value.capacity();
        final Value prev = map.put(key, new Value(System.currentTimeMillis(), value));
        if (prev == null) {
            // + key and timestamp
            sizeInBytes += key.capacity() + Long.BYTES;
        } else if (!prev.isTombstone()) {
            // - old value
            sizeInBytes -= prev.getData().capacity();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value prev = map.put(key, new Value(System.currentTimeMillis(), null));
        if (prev == null) {
            // + key and timestamp
            sizeInBytes += key.capacity() + Long.BYTES;
        } else if (!prev.isTombstone()) {
            // - old value
            sizeInBytes -= prev.getData().capacity();
        }
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void close() throws IOException {
        map.clear();
        sizeInBytes = 0;
    }
}
