package ru.mail.polis.dao.impl.async;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.tables.MemTable;
import ru.mail.polis.dao.impl.tables.Table;

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

    @NotNull
    public TableSet markAsFlushing() {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        flushing.add(memTable);
        return new TableSet(new MemTable(), flushing, ssTables, this.generation + 1);
    }

    @NotNull
    public TableSet moveToFlushedFiles(@NotNull final Table memTable,
                                       @NotNull final Table ssTable,
                                       final long generation) {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        if (!flushing.remove(memTable)) {
            throw new IllegalStateException();
        }
        final NavigableMap<Long, Table> ssTables = new TreeMap<>(this.ssTables);
        if (ssTables.put(generation, ssTable) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, flushing, ssTables, this.generation);
    }

    @NotNull
    public TableSet compacting() {
        return new TableSet(memTable, flushing, ssTables, this.generation + 1);
    }

    @NotNull
    public TableSet replaceCompactedFiles(@NotNull final NavigableMap<Long, Table> source,
                                                 @NotNull final Table dest,
                                                 final long generation) {
        final NavigableMap<Long, Table> ssTables = new TreeMap<>(this.ssTables);
        for (final var entry : source.entrySet()) {
            if (!ssTables.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException();
            }
        }
        if (ssTables.put(generation, dest) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, this.flushing, ssTables, this.generation);
    }

}
