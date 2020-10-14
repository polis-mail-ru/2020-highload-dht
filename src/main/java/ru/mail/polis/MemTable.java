package ru.mail.polis;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class MemTable implements Table {

    private final NavigableMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();
    private final AtomicLong size = new AtomicLong();

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(
                map.tailMap(from).entrySet().iterator(),
                e -> new Cell(e.getKey(), e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        map.put(key.duplicate(), new Value(System.currentTimeMillis(), value.duplicate()));
        size.addAndGet(key.remaining() + value.remaining() + Long.BYTES);

    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        if (map.containsKey(key)) {
            if (!map.get(key).isRemoved()) {
                size.addAndGet(-map.get(key).getData().remaining());
            }
        } else {
            size.addAndGet(key.remaining() + Long.BYTES);
        }
        map.put(key, new Value(System.currentTimeMillis()));
    }

    @Override
    public long size() {
        return size.get();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    public int mapSize() {
        return map.size();
    }
}
