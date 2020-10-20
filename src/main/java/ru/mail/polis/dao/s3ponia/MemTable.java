package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class MemTable implements Table {
    private final SortedMap<ByteBuffer, Table.Value> keyToRecord;
    private final int generation;

    @Override
    public int getGeneration() {
        return generation;
    }

    public MemTable(final int generation) {
        this.keyToRecord = new ConcurrentSkipListMap<>();
        this.generation = generation;
    }

    @Override
    public int size() {
        return keyToRecord.entrySet().stream()
                .map(a -> a.getKey().capacity() + a.getValue().size())
                .reduce(0, Integer::sum);
    }

    @Override
    public Iterator<Table.ICell> iterator() {
        return keyToRecord.entrySet().stream().map(e -> Table.Cell.of(e.getKey(), e.getValue()))
                .map(c -> (Table.ICell) c).iterator();
    }

    /**
     * Provides iterator (possibly empty) over {@link Table.Cell}s starting at "from" key (inclusive)
     * in <b>ascending</b> order according to {@link Table.Cell#compareTo(Table.ICell)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    @Override
    public Iterator<Table.ICell> iterator(@NotNull final ByteBuffer from) {
        return keyToRecord.tailMap(from).entrySet().stream().map(
                e -> Table.Cell.of(e.getKey(), e.getValue())
        ).map(c -> (Table.ICell) c).iterator();
    }

    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) {
        final var val = keyToRecord.get(key);
        return val == null ? null : val.getValue();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        keyToRecord.put(key, Table.Value.of(value, generation));
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        keyToRecord.put(key, Table.Value.dead(generation));
    }

    @Override
    public void close() {
        keyToRecord.clear();
    }
}
