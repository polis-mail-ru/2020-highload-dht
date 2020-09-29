package ru.mail.polis.dao.gogun;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

final class MemTable implements Table {

    private final NavigableMap<ByteBuffer, Value> map = new TreeMap<>();
    private long sizeInBytes;
    private int size;

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
            this.sizeInBytes += key.remaining() + value.remaining() + Long.BYTES;
        } else {
            this.sizeInBytes += value.remaining() - valueToCheck.getData().remaining();
        }

        size = map.size();
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value value = map.put(key.duplicate(), new Value(System.currentTimeMillis()));
        if (value == null) {
            this.sizeInBytes += Long.BYTES + key.remaining();
        }
        if (value != null && !value.isTombstone()) {
            this.sizeInBytes -= value.getData().remaining();
        }

        size = map.size();
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() {
        map.clear();
    }

    public int getSize() {
        return size;
    }
}
