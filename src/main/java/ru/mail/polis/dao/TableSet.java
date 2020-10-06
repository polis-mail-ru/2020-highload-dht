package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TableSet {
    // Data
    @NotNull
    final Table memTable; // RW
    @NotNull
    final Set<Table> flushingTables; // Read-only memTables
    @NotNull
    final NavigableMap<Integer, Table> ssTables; // Read only ssTables

    // State
    final int generation;

    public TableSet(
        @NotNull Table memTable,
        @NotNull Set<Table> flushingTables,
        @NotNull NavigableMap<Integer, Table> ssTables,
        int generation) {

        assert generation >= 0;
        this.memTable = memTable;
        this.flushingTables = Collections.unmodifiableSet(flushingTables);
        this.ssTables = Collections.unmodifiableNavigableMap(ssTables);
        this.generation = generation;
    }

    @NotNull
    static TableSet fromFiles(
        @NotNull NavigableMap<Integer, Table> ssTables,
        final int generation) {

        return new TableSet(new MemTable(), Collections.emptySet(), ssTables, generation);
    }

    @NotNull
    TableSet markMemTableAsFlushing() {
        final Set<Table> flushing = new HashSet<>(this.flushingTables);
        flushing.add(memTable);
        return new TableSet(new MemTable(), flushing, ssTables, generation + 1);
    }

    @NotNull
    TableSet moveFlushingMemTableToFlushedFiles(
        @NotNull final Table memTable,
        @NotNull final Table file,
        final int generation) {

        final Set<Table> flushing = new HashSet<>(this.flushingTables);
        if (!flushing.remove(memTable)) {
            throw new IllegalStateException("This table isn't flushing now");
        }
        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTables);
        if (files.put(generation, file) != null) {
            throw new IllegalStateException("Collision of generation");
        }
        return new TableSet(this.memTable, flushing, files, this.generation);
    }

    @NotNull
    TableSet startCompacting() {
        return new TableSet(memTable, flushingTables, ssTables, generation + 1);
    }

    @NotNull
    TableSet replaceCompactedFiles(
        @NotNull final NavigableMap<Integer, Table> source,
        @NotNull final Table destination,
        final int generation) {

        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTables);
        for (final Map.Entry<Integer, Table> entry : source.entrySet()) {
            if (!files.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("This file doesn't exist!");
            }
        }
        if (files.put(generation, destination) != null) {
            throw new IllegalStateException("Collision of generation");
        }

        return new TableSet(this.memTable, this.flushingTables, files, this.generation);
    }
}
