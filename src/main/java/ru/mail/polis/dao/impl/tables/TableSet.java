package ru.mail.polis.dao.impl.tables;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class TableSet {

    @NotNull
    public Table memTable;
    @NotNull
    public Set<Table> flushing;
    @NotNull
    public final NavigableMap<Long, Table> ssTables;
    public final long generation;

    private TableSet(@NotNull final Table memTable,
                    @NotNull final Set<Table> flushing,
                    @NotNull final NavigableMap<Long, Table> ssTables,
                    final long generation) {
        this.memTable = memTable;
        this.flushing = Collections.unmodifiableSet(flushing);
        this.ssTables = Collections.unmodifiableNavigableMap(ssTables);
        this.generation = generation;
    }

    @NotNull
    public static TableSet fromFiles(@NotNull final NavigableMap<Long, Table> ssTables,
                            final long generation) {
        return new TableSet(new MemTable(), Collections.emptySet(), ssTables, generation);
    }

    /** Mark current memTable as "to be flushed".
     * @return TableSet of flushing memTables
     * */
    @NotNull
    public TableSet markAsFlushing() {
        final Set<Table> flushingFiles = new HashSet<>(this.flushing);
        flushingFiles.add(memTable);
        return new TableSet(new MemTable(), flushingFiles, ssTables, this.generation + 1);
    }

    /** Move flushed memTable to flushed files.
     * @param memTable - flushed memTable
     * @param ssTable - current file table
     * @param generation - current generation
     * @return TableSet of flushed memTables and file tables
     * */
    @NotNull
    public TableSet moveToFlushedFiles(@NotNull final Table memTable,
                                       @NotNull final Table ssTable,
                                       final long generation) {
        final Set<Table> flushingFiles = new HashSet<>(this.flushing);
        if (!flushingFiles.remove(memTable)) {
            throw new IllegalStateException();
        }
        final NavigableMap<Long, Table> files = new TreeMap<>(this.ssTables);
        if (files.put(generation, ssTable) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, flushingFiles, files, this.generation);
    }

    @NotNull
    public TableSet compacting() {
        return new TableSet(memTable, flushing, ssTables, this.generation + 1);
    }

    /** Move flushed memTable to flushed files.
     * @param source - compacted file tables
     * @param dest - memTable to replace compacted files
     * @param generation - current generation
     * @return TableSet of replaced compacted files
     * */
    @NotNull
    public TableSet replaceCompactedFiles(@NotNull final NavigableMap<Long, Table> source,
                                                 @NotNull final Table dest,
                                                 final long generation) {
        final NavigableMap<Long, Table> files = new TreeMap<>(this.ssTables);
        for (final var entry : source.entrySet()) {
            if (!files.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException();
            }
        }
        if (files.put(generation, dest) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, this.flushing, files, this.generation);
    }

}
