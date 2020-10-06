package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.MemTable;
import ru.mail.polis.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TableSet {
    public Set<Table> readyToFlush;
    public final NavigableMap<Integer, Table> ssTable;
    public MemTable memTable;
    public int generation;

    public TableSet(
            @NotNull final MemTable memTable,
            @NotNull final Set<Table> readyToFlush,
            @NotNull final NavigableMap<Integer, Table> ssTable,
            final int generation) {
        this.memTable = memTable;
        this.readyToFlush = Collections.unmodifiableSet(readyToFlush);
        this.ssTable = Collections.unmodifiableNavigableMap(ssTable);
        this.generation = generation;
    }

    public TableSet(@NotNull final NavigableMap<Integer, Table> ssTables, final int generation) {
        assert generation >= 0;
        this.ssTable = ssTables;
        this.readyToFlush = new HashSet<>();
        this.memTable = new MemTable();
        this.generation = generation;
    }

    @NotNull
    public TableSet addToFlush() {
        final Set<Table> readyToFlush =
                new HashSet<>(this.readyToFlush);
        readyToFlush.add(memTable);
        return new TableSet(new MemTable(), readyToFlush, ssTable, this.generation + 1);
    }

    @NotNull
    public TableSet flush(
            @NotNull final Table memTable,
            @NotNull final Table ssTable,
            final int gen) {
        final Set<Table> readyToFlush = new HashSet<>(this.readyToFlush);
        if (!readyToFlush.remove(memTable)) {
            throw new IllegalStateException();
        }
        final NavigableMap<Integer, Table> navigableSsTable = new TreeMap<>(this.ssTable);
        if (navigableSsTable.put(gen, ssTable) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, readyToFlush, navigableSsTable, this.generation);
    }

    @NotNull
    public TableSet flushCompactTable(
            @NotNull final NavigableMap<Long, Table> base,
            @NotNull final Table dest,
            final int generation) {
        final NavigableMap<Integer, Table> ssTable = new TreeMap<>(this.ssTable);
        for (final var entry : base.entrySet()) {
            if (!ssTable.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException();
            }
        }
        if (ssTable.put(generation, dest) != null) {
            throw new IllegalStateException();
        }
        return new TableSet(this.memTable, this.readyToFlush, ssTable, this.generation);
    }

    @NotNull
    public TableSet flushSsTable() {
        return new TableSet(memTable, readyToFlush, ssTable, this.generation + 1);
    }
}
