package ru.mail.polis.dao.dmrnz;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

class MemTable implements Table {
    @NotNull
    private final NavigableMap<ByteBuffer, Value> storage = new TreeMap<>();
    private long sizeInBytes;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(
                storage.tailMap(from).entrySet().iterator(),
                e -> new Cell(e.getKey(), e.getValue()));
    }

    @NotNull
    @Override
    public Iterator<Cell> reverseIterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(
                storage.headMap(from, true).descendingMap().entrySet().iterator(),
                e -> new Cell(e.getKey(), e.getValue())
        );
    }

    @NotNull
    @Override
    public Iterator<Cell> reverseIterator() {
        return Iterators.transform(
                storage.descendingMap().entrySet().iterator(),
                e -> new Cell(e.getKey(), e.getValue())
        );
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value) {
        final Value previous = storage.put(key, Value.of(value));
        if (previous == null) {
            sizeInBytes += key.remaining() + value.remaining();
        } else if (previous.isRemoved()) {
            sizeInBytes += value.remaining();
        } else {
            sizeInBytes += value.remaining() - previous.getData().remaining();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value previous = storage.put(key, Value.tombstone());
        if (previous == null) {
            sizeInBytes += key.remaining();
        } else if (!previous.isRemoved()) {
            sizeInBytes -= previous.getData().remaining();
        }
    }

    @Override
    public long sizeInBytes() {
        return sizeInBytes;
    }
}
