package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class TableSet {
    @NotNull
    public Set<Table> tablesReadyToFlush;
    @NotNull
    public final NavigableMap<Long, Table> ssTableCollection;
    @NotNull
    public Table currMemTable;

    public final long gen;

    public TableSet(
            @NotNull final Table currMemTable,
            @NotNull final Set<Table> tablesReadyToFlush,
            @NotNull final NavigableMap<Long, Table> ssTableCollection,
            final long gen) {
        this.currMemTable = currMemTable;
        this.tablesReadyToFlush =
                Collections.unmodifiableSet(tablesReadyToFlush);
        this.ssTableCollection =
                Collections.unmodifiableNavigableMap(ssTableCollection);
        this.gen = gen;
    }

    @NotNull
    public TableSet setToFlush() {
        final Set<Table> tablesToFlush =
                new HashSet<>(
                        this.tablesReadyToFlush);
        tablesToFlush.add(currMemTable);
        return
                new TableSet(
                        new MemTable(),
                        tablesToFlush,
                        ssTableCollection,
                        this.gen + 1);
    }

    @NotNull
    public TableSet flush(
            @NotNull final Table memTable,
            @NotNull final Table ssTable,
            final long gen) {
        final Set<Table> tablesToFlush =
                new HashSet<>(this.tablesReadyToFlush);
        if (!tablesToFlush.remove(memTable)) {
            throw new IllegalStateException("memTable закрыта!");
        }
        final NavigableMap<Long, Table> ssTableCollection =
                new TreeMap<>(this.ssTableCollection);
        if (ssTableCollection.put(gen, ssTable) != null) {
            throw new IllegalStateException("memTable закрыта!");
        }
        return
                new TableSet(
                        this.currMemTable,
                        tablesToFlush,
                        ssTableCollection,
                        this.gen);
    }

    @NotNull
    public TableSet flushCompactTable(
            @NotNull final NavigableMap<Long, Table> base,
            @NotNull final Table dest,
            final long gen) {
        final NavigableMap<Long, Table> ssTableCollection =
                new TreeMap<>(this.ssTableCollection);
        for (final var entry : base.entrySet()) {
            if (!ssTableCollection.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("Ошибка compact");
            }
        }
        if (ssTableCollection.put(gen, dest) != null) {
            throw new IllegalStateException("Ошибка compact");
        }
        return
                new TableSet(
                        this.currMemTable,
                        this.tablesReadyToFlush,
                        ssTableCollection,
                        this.gen);
    }

    @NotNull
    public TableSet compactSSTables() {
        return
                new TableSet(
                        currMemTable,
                        tablesReadyToFlush,
                        ssTableCollection,
                        this.gen + 1);
    }
}
