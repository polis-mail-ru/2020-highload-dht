package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class MemTable implements Table {

    private final SortedMap<ByteBuffer, Value> map;
    private final AtomicLong sizeInBytes;

    public MemTable() {
        map = new ConcurrentSkipListMap<>();
        sizeInBytes = new AtomicLong(0);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map((entry) -> new Cell(entry.getKey().duplicate(), entry.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        sizeInBytes.addAndGet(value.capacity());
        final Value prev = map.put(key.duplicate(), new Value(System.currentTimeMillis(), value.duplicate()));
        if (prev == null) {
            // + key and timestamp
            sizeInBytes.addAndGet(key.capacity() + Long.BYTES);
        } else if (!prev.isTombstone()) {
            // - old value
            sizeInBytes.addAndGet(-prev.getData().capacity());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value prev = map.put(key.duplicate(), new Value(System.currentTimeMillis(), null));
        if (prev == null) {
            // + key and timestamp
            sizeInBytes.addAndGet(key.duplicate().capacity() + Long.BYTES);
        } else if (!prev.isTombstone()) {
            // - old value
            sizeInBytes.addAndGet(-prev.getData().capacity());
        }
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes.get();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void close() throws IOException {
        map.clear();
        sizeInBytes.set(0);
    }
}
