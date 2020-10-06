package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

final class TableSet {
    @NotNull
    final MemTable memTable;
    @NotNull
    final Set<Table> flushing;
    @NotNull
    final NavigableMap<Integer, SSTable> ssTables;
    final int generation;

    private TableSet(@NotNull MemTable memTable, @NotNull Set<Table> flushing,
                     @NotNull NavigableMap<Integer, SSTable> ssTables, int generation) {
        this.memTable = memTable;
        this.flushing = flushing;
        this.ssTables = ssTables;
        this.generation = generation;
    }

    @NotNull
    static TableSet fromFiles(@NotNull NavigableMap<Integer, SSTable> ssTables, int generation) {
        return new TableSet(new MemTable(), Collections.emptySet(), ssTables, generation);
    }

    @NotNull
    TableSet flushing() {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        flushing.add(this.memTable);
        return new TableSet(new MemTable(), flushing, ssTables, this.generation + 1);
    }

    @NotNull
    TableSet flushed(@NotNull final MemTable memTable, @NotNull final SSTable ssTable, int generation) {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        if (!flushing.remove(memTable)) {
            throw new IllegalStateException("error");
        }
        final NavigableMap<Integer, SSTable> files = new TreeMap<>(this.ssTables);
        if (files.put(generation, ssTable) != null) {
            throw new IllegalStateException("generation collision");
        }

        return new TableSet(this.memTable, flushing, files, this.generation);
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
        final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>(this.ssTables);
        for (final Map.Entry<Integer, SSTable> entry : source.entrySet()) {
            if(!ssTables.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("error");
            }
        }
        if (ssTables.put(generation, destination) != null) {
            throw new IllegalStateException("generation collision");
        }



        return new TableSet(this.memTable, flushing, ssTables, this.generation);
    }
}
