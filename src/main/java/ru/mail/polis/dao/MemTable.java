package ru.mail.polis.dao;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

final class MemTable implements Table {
    private final NavigableMap<ByteBuffer, Value> map;
    private long sizeInBytes;

    public MemTable() {
        map = new TreeMap<>();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(map.tailMap(from).entrySet().iterator(),
                entry -> new Cell(entry.getKey(), entry.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value previous = map.put(key, new Value(System.currentTimeMillis(), value));
        sizeInBytes += value.remaining();
        if (previous == null) {
            sizeInBytes += key.remaining() + Long.BYTES;
        } else if (!previous.isTombstone()) {
            // sizeInBytes += difference between new data and old data
            sizeInBytes -= previous.getData().remaining();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value previous = map.put(key, Value.tombstone(System.currentTimeMillis()));
        if (previous == null) {
            sizeInBytes += key.remaining();
        } else if (!previous.isTombstone()) {
            sizeInBytes -= previous.getData().remaining();
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
    public void close() {
        // do nothing
    }
}
