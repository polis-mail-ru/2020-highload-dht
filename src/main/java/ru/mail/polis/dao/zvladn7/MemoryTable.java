package ru.mail.polis.dao.zvladn7;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
public class MemoryTable implements Table {

    private final SortedMap<ByteBuffer, Value> map;
    private final AtomicInteger currentAmountOfBytes;

    public int getAmountOfBytes() {
        return currentAmountOfBytes.get();
    }

    MemoryTable() {
        this.map = new ConcurrentSkipListMap<>();
        this.currentAmountOfBytes = new AtomicInteger();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey(), entry.getValue()))
                .iterator();
    }

    @Nullable
    ByteBuffer get(@NotNull final ByteBuffer key) {
        final Value value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.getData();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value val = map.put(key.duplicate(), new Value(System.currentTimeMillis(), value.duplicate()));
        if (val == null) {
            currentAmountOfBytes.addAndGet(key.remaining() + value.remaining() + Long.BYTES);
        } else {
            currentAmountOfBytes.addAndGet(value.remaining() - val.getData().remaining());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value value = map.put(key.duplicate(), Value.newTombstoneValue(System.currentTimeMillis()));
        if (value == null) {
            currentAmountOfBytes.addAndGet(key.remaining() + Long.BYTES);
        } else if (!value.isTombstone()) {
            currentAmountOfBytes.addAndGet(value.getData().remaining());
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void close() {
        //nothing to close
    }

    public void clear() {
        map.clear();
    }
}
