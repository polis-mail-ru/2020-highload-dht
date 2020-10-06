package ru.mail.polis.dao.bmendli;

import javax.annotation.concurrent.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public final class MemTable implements Table {

    @NotNull
    private final SortedMap<ByteBuffer, Value> map;
    private final AtomicLong size;

    /**
     * Memory storage for new data.
     */
    public MemTable() {
        map = new ConcurrentSkipListMap<>();
        size = new AtomicLong(0);
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value, final long expireTime) {
        final Value removedValue = map.put(key, Value.newInstance(value, System.currentTimeMillis(), expireTime));
        size.addAndGet(value.remaining());
        if (removedValue == null) {
            size.addAndGet(key.remaining() + Long.BYTES + Long.BYTES);
        } else if (!removedValue.isTombstone()) {
            size.addAndGet(-removedValue.getData().duplicate().remaining());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value removedValue = map.put(key, Value.newInstance());
        if (removedValue == null) {
            size.addAndGet(key.remaining() + Long.BYTES + Long.BYTES);
        } else if (!removedValue.isTombstone()) {
            size.addAndGet(-removedValue.getData().duplicate().remaining());
        }
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map
                .tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey().duplicate(), entry.getValue().isExpired()
                        ? Value.newInstance()
                        : entry.getValue()))
                .iterator();
    }

    @Override
    public long size() {
        return size.get();
    }

    @Override
    public void close() {
        map.clear();
        size.set(0);
    }
}
