package ru.mail.polis.dao.kirkungurov;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {

    @NotNull
    private final SortedMap<ByteBuffer, Value> map;
    private long size;

    public MemTable() {
        this.map = new TreeMap<>();
        this.size = 0;
    }

    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey(), entry.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        @Nullable final Value oldValue = map.get(key);
        if (oldValue == null) {
            size += key.remaining() + value.remaining() + Long.BYTES;
        } else {
            size += value.remaining() - oldValue.getData().remaining();
        }
        map.put(key.duplicate(), new Value(value.duplicate()));
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        @Nullable final Value oldValue = map.get(key);
        if (oldValue == null) {
            size += key.remaining() + Long.BYTES;
        } else if (!oldValue.isTombstone()) {
            size -= oldValue.getData().remaining();
        }
        map.put(key.duplicate(), new Value(System.currentTimeMillis()));
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
