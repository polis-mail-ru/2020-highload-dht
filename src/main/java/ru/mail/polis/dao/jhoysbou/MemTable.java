package ru.mail.polis.dao.jhoysbou;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {
    private final SortedMap<ByteBuffer, Value> map = new TreeMap<>();
    private long sizeInBytes;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(element -> new Cell(element.getKey(), element.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final Value oldValue = map.put(key.duplicate(), new Value(System.currentTimeMillis(), value));

        if (oldValue == null) {
            sizeInBytes += key.remaining() + value.remaining() + Long.BYTES;
        } else {
            sizeInBytes += value.remaining() - oldValue.getData().remaining();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final Value oldValue = map.put(key.duplicate(), new Value(System.currentTimeMillis()));

        if (oldValue == null) {
            sizeInBytes += key.remaining() + Long.BYTES;
        } else {
            sizeInBytes -= oldValue.getData().remaining();
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public void close() {
        // no need to close something
    }
}
