package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.util.*;

final class TableSet {
    @NotNull
    final MemTable memTable;
    @NotNull
    final Set<Table> flushing;
    @NotNull
    final NavigableMap<Integer, SSTable> ssTables;
    final int generation;

    private TableSet(@NotNull final MemTable memTable, @NotNull final Set<Table> flushing,
                     @NotNull final NavigableMap<Integer, SSTable> ssTables, final int generation) {
        this.memTable = memTable;
        this.flushing = flushing;
        this.ssTables = ssTables;
        this.generation = generation;
    }

    @NotNull
    static TableSet fromFiles(@NotNull final NavigableMap<Integer, SSTable> ssTables, final int generation) {
        return new TableSet(new MemTable(), Collections.emptySet(), ssTables, generation);
    }

    @NotNull
    TableSet startFlushing() {
        final Set<Table> newMemTable = new HashSet<>(this.flushing);
        newMemTable.add(this.memTable);
        return new TableSet(new MemTable(), newMemTable, ssTables, this.generation + 1);
    }

    @NotNull
    TableSet flushed(@NotNull final MemTable memTable, @NotNull final SSTable ssTable, final int generation) {
        final Set<Table> newMemTable = new HashSet<>(this.flushing);
        if (!newMemTable.remove(memTable)) {
            throw new IllegalStateException("error");
        }
        final NavigableMap<Integer, SSTable> files = new TreeMap<>(this.ssTables);

        if (files.put(generation, ssTable) != null) {
            throw new IllegalStateException("generation collision");
        }

        return new TableSet(this.memTable, newMemTable, files, this.generation);
    }

    @NotNull
    TableSet startCompaction() {
        return new TableSet(memTable, flushing, ssTables, generation + 1);
    }

    @NotNull
    TableSet replaceCompactedFiles(
            @NotNull final NavigableMap<Integer, SSTable> source,
            @NotNull final SSTable destination,
            final int generation) {
        final NavigableMap<Integer, SSTable> newSsTables = new TreeMap<>(this.ssTables);
        for (final Map.Entry<Integer, SSTable> entry : source.entrySet()) {
            if (!newSsTables.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("error");
            }
        }
        if (newSsTables.put(generation, destination) != null) {
            throw new IllegalStateException("generation collision");
        }

        return new TableSet(this.memTable, flushing, newSsTables, this.generation);
    }
}
