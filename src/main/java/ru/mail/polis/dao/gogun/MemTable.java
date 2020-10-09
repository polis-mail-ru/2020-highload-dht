package ru.mail.polis.dao.gogun;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

final class MemTable implements Table {

    private final NavigableMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();
    private final AtomicLong sizeInBytes = new AtomicLong();

    @NotNull
    @Override
    public Iterator<Row> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(
                map.tailMap(from)
                        .entrySet()
                        .iterator(),
                e -> new Row(e.getKey(), e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final Value valueToCheck = map.put(key.duplicate(), new Value(System.currentTimeMillis(), value.duplicate()));
        if (valueToCheck == null) {
            this.sizeInBytes.addAndGet(key.remaining() + value.remaining() + Long.BYTES);
        } else {
            this.sizeInBytes.addAndGet(value.remaining() - valueToCheck.getData().remaining());
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value value = map.put(key.duplicate(), new Value(System.currentTimeMillis()));
        if (value == null) {
            this.sizeInBytes.addAndGet(Long.BYTES + key.remaining());
        }
        if (value != null && !value.isTombstone()) {
            this.sizeInBytes.addAndGet(-value.getData().remaining());
        }
    }

    public long getSizeInBytes() {
        return sizeInBytes.get();
    }

    @Override
    public void close() {
        map.clear();
    }

    public int getSize() {
        return map.size();
    }
}
