package ru.mail.polis.dao.bmendli;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public final class MemTable implements Table {

    @NotNull
    private final SortedMap<ByteBuffer, Value> map;
    private long size;

    /**
     * Memory storage for new data.
     */
    public MemTable() {
        map = new TreeMap<>();
        size = 0;
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value, final long expireTime) {
        final Value removedValue = map.put(key, Value.newInstance(value, System.currentTimeMillis(), expireTime));
        size += value.remaining();
        if (removedValue == null) {
            size += key.remaining() + Long.BYTES + Long.BYTES;
        } else if (!removedValue.isTombstone()) {
            size -= removedValue.getData().remaining();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value removedValue = map.put(key, Value.newInstance());
        if (removedValue == null) {
            size += key.remaining() + Long.BYTES + Long.BYTES;
        } else if (!removedValue.isTombstone()) {
            size -= removedValue.getData().remaining();
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map
                .tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey(), entry.getValue().isExpired()
                        ? Value.newInstance()
                        : entry.getValue()))
                .iterator();
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void close() {
        map.clear();
        size = 0;
    }
}
