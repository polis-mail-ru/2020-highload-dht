package ru.mail.polis;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {

    private final SortedMap<ByteBuffer, Value> map = new TreeMap<>();
    private long size;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(
                map.tailMap(from).entrySet().iterator(),
                e -> new Cell(e.getKey(), e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        map.put(key.duplicate(), new Value(Time.getCurrentTime(), value.duplicate()));
        size += key.remaining() + value.remaining() + Long.BYTES;
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value previous = map.put(key, Value.tombstone());
        if (previous == null) {
            size += key.remaining();
        } else if (!previous.isRemoved()) {
            size -= previous.getData().remaining();
        }
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }
}
