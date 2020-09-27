package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class MemTable implements Table {
    @NotNull
    private final SortedMap<ByteBuffer, Value> map;
    private long size;

    MemTable() {
        map = new ConcurrentSkipListMap<>();
        size = 0;
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        @Nullable final Value removedValue = map.put(key, new Value(value));
        size += value.remaining();
        changeSize(removedValue, key);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        @Nullable final Value removedValue = map.put(key, new Value());
        changeSize(removedValue, key);
    }

    private void changeSize(final Value removedValue, @NotNull final ByteBuffer key) {
        if (removedValue == null) {
            size += key.remaining() + Long.BYTES;
        } else if (!removedValue.isTombstone()) {
            size -= removedValue.getData().remaining();
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(element -> new Cell(element.getKey(), element.getValue().getData()))
                .iterator();
    }

    @Override
    public long sizeInBytes() {
        return size;
    }

    @Override
    public void close() {
        map.clear();
        size = 0;
    }
}
