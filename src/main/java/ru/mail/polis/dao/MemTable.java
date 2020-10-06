package ru.mail.polis.dao;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
final class MemTable implements Table {
    private final NavigableMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();
    private final AtomicLong sizeInBytes = new AtomicLong();

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(map.tailMap(from).entrySet().iterator(),
                entry -> new Cell(entry.getKey(), entry.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value previous = map.put(key, new Value(System.currentTimeMillis(), value));
        sizeInBytes.addAndGet(value.remaining());
        if (previous == null) {
            sizeInBytes.addAndGet(key.remaining() + Long.BYTES);
        } else if (!previous.isTombstone()) {
            // sizeInBytes += difference between new data and old data
            sizeInBytes.addAndGet(-previous.getData().remaining());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value previous = map.put(key, Value.tombstone(System.currentTimeMillis()));
        if (previous == null) {
            sizeInBytes.addAndGet(key.remaining());
        } else if (!previous.isTombstone()) {
            sizeInBytes.addAndGet(-previous.getData().remaining());
        }
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes.get();
    }

    @Override
    public long size() {
        return map.size();
    }

    @Override
    public void close() {
        // do nothing
    }
}
