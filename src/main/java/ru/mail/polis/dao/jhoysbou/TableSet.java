package ru.mail.polis.dao.jhoysbou;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TableSet {
    @NotNull
    final Table memTable;
    @NotNull
    final Set<Table> flushingTables;
    @NotNull
    final NavigableMap<Integer, Table> ssTables;
    final int generation;

    private TableSet(@NotNull final Table memTable,
                     @NotNull final Set<Table> flushingTables,
                     @NotNull final NavigableMap<Integer, Table> files,
                     final int generation) {
        assert generation >= 0;

        this.memTable = memTable;
        this.flushingTables = Collections.unmodifiableSet(flushingTables);
        this.ssTables = Collections.unmodifiableNavigableMap(files);
        this.generation = generation;
    }

    @NotNull
    static TableSet fromFiles(@NotNull final NavigableMap<Integer, Table> files,
                       final int generation) {
        return new TableSet(new MemTable(),
                Collections.emptySet(),
                files, generation);
    }

    @NotNull
    TableSet flushing() {
        final Set<Table> flushing = new HashSet<>(this.flushingTables);
        flushing.add(memTable);

        return new TableSet(new MemTable(), flushing, ssTables, this.generation + 1);
    }

    @NotNull
    TableSet flushed(@NotNull final Table memTable,
                     final int generation,
                     @NotNull final Table file) {
        final HashSet<Table> flushing = new HashSet<>(this.flushingTables);
        if (!flushing.remove(memTable)) {
            throw new IllegalStateException(
                    "Tried to set status 'flushed' to a table that has not been in status 'flushing'!"
            );
        }

        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTables);
        if (files.put(generation, file) != null) {
            throw new IllegalStateException("There is already one file with generation = " + generation);
        }

        return new TableSet(this.memTable, flushing, files, this.generation);
    }

    @NotNull
    TableSet startCompaction() {
        return new TableSet(this.memTable, this.flushingTables, this.ssTables, this.generation + 1);
    }

    @NotNull
    TableSet replaceCompactedFiles(@NotNull final NavigableMap<Integer, Table> source,
                                   @NotNull final Table destination,
                                   final int generation) {
        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTables);
        for (final Map.Entry<Integer, Table> entry : source.entrySet()) {
            if (!files.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("Compacted file with generation = " + entry.getKey() + " disappeared");
            }
        }

        if (files.put(generation, destination) != null) {
            throw new IllegalStateException("There is already one file with generation = " + generation);
        }

        return new TableSet(this.memTable, this.flushingTables, files, this.generation);
    }

}
