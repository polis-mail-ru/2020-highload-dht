package ru.mail.polis.dao.suhova;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class MemTable implements Table {
    private final NavigableMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();
    private final AtomicLong size = new AtomicLong();

    public int getEntryCount() {
        return map.size();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(
            map.tailMap(from).entrySet().iterator(),
            e -> new Cell(Objects.requireNonNull(e).getKey(), e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value prev = map.put(key.duplicate(), new Value(value.duplicate(), System.currentTimeMillis()));
        if (prev == null) {
            size.getAndAdd(key.remaining() + value.remaining() + Long.BYTES);
        } else if (prev.getData() != null) {
            size.getAndAdd(value.remaining() - prev.getData().remaining());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value prev = map.put(key, Value.tombstone(System.currentTimeMillis()));
        if (prev == null) {
            size.getAndAdd(key.remaining() + Long.BYTES);
        } else {
            if (!prev.isTombstone()) {
                size.getAndAdd(-prev.getData().remaining());
            }
        }
    }

    @Override
    public long sizeInBytes() {
        return size.get();
    }

    @Override
    public void close() {
        //nothing to close:)
    }
}
