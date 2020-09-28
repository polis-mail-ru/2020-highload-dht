package ru.mail.polis.dao.kuzoliza;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class MemTable implements Table {

    private final SortedMap<ByteBuffer, Value> map = new TreeMap<>();
    private long sizeInBytes;
    private int size;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException {
        return Iterators.transform(
                map.tailMap(from).entrySet().iterator(),
                element -> {
                    assert element != null;
                    return new Cell(element.getKey(), element.getValue());
                });
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value newValue = new Value(System.currentTimeMillis(), value.duplicate());
        final Value previous = map.put(key.duplicate(), newValue);

        if (previous == null) {
            sizeInBytes += countSize(key, newValue);
            size++;
        } else {
            sizeInBytes += countSize(key, newValue) - countSize(key, previous);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value value = new Value(System.currentTimeMillis());
        final Value previous = map.put(key.duplicate(), new Value(System.currentTimeMillis()));

        if (previous == null) {
            sizeInBytes += countSize(key, value);
            size++;
        } else {
            sizeInBytes += countSize(key, value) - countSize(key, previous);
        }
    }

    long sizeInBytes() {
        return sizeInBytes;
    }

    private static long countSize(final ByteBuffer key, final Value value) {
        final ByteBuffer size = value.getData();
        if (size != null) {
            return 128L + key.remaining() + value.getData().remaining();
        }
        return 128L + key.remaining();
    }

    public int size() {
        return size;
    }

}
