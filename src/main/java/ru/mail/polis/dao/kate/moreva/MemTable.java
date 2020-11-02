package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class MemTable implements Table {
    @NotNull
    private final SortedMap<ByteBuffer, Value> map;
    private final AtomicLong size = new AtomicLong();

    MemTable() {
        map = new ConcurrentSkipListMap<>();
        size.set(0);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        @Nullable final Value removedValue = map.put(key, new Value(value));
        size.addAndGet(value.remaining());
        changeSize(removedValue, key);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        @Nullable final Value removedValue = map.put(key, new Value());
        changeSize(removedValue, key);
    }

    private void changeSize(final Value removedValue, @NotNull final ByteBuffer key) {
        if (removedValue == null) {
            size.addAndGet(key.remaining() + Long.BYTES);
        } else if (!removedValue.isTombstone()) {
            size.addAndGet(-removedValue.getData().duplicate().remaining());
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(element -> new Cell(element.getKey(), element.getValue()))
                .iterator();
    }

    @Override
    public long sizeInBytes() {
        return size.get();
    }

    @Override
    public void close() {
        map.clear();
        size.set(0);
    }
}
