package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class TableSet {
    @NotNull
    public Set<Table> flushingTables;

    @NotNull
    public final NavigableMap<Integer, Table> diskTables;

    @NotNull
    public Table memTable;

    public final int generation;

    /**
     * Constructor {link TableSet}.
     *
     * @param currMemTable       current MemTable
     * @param tablesReadyToFlush flushing tables
     * @param diskTableCollection  flushed tables
     * @param generation                generation
     */
    public TableSet(
            @NotNull final Table currMemTable,
            @NotNull final Set<Table> tablesReadyToFlush,
            @NotNull final NavigableMap<Integer, Table> diskTableCollection,
            final int generation) {
        this.memTable = currMemTable;
        this.flushingTables =
                Collections.unmodifiableSet(tablesReadyToFlush);
        this.diskTables =
                Collections.unmodifiableNavigableMap(diskTableCollection);
        this.generation = generation;
    }

    /**
     * Set current table to flushing.
     *
     * @return TableSet that ready to flush
     */
    @NotNull
    public TableSet beforeFlush() {
        final Set<Table> tablesToFlush = new HashSet<>(this.flushingTables);
        tablesToFlush.add(memTable);
        return new TableSet(new MemTable(this.generation + 1),
                tablesToFlush,
                diskTables,
                this.generation + 1);
    }

    /**
     * Set flushing table to flushed.
     *
     * @param table     flushing table
     * @param diskTable flushed table
     * @param gen       generation
     * @return new TableSet
     */
    @NotNull
    public TableSet afterFlush(
            @NotNull final Table table,
            @NotNull final Table diskTable,
            final int gen) {
        final Set<Table> tablesToFlush =
                new HashSet<>(this.flushingTables);
        if (!tablesToFlush.remove(table)) {
            throw new IllegalStateException("table have already flushed.");
        }
        final NavigableMap<Integer, Table> newDiskTables =
                new TreeMap<>(this.diskTables);
        if (newDiskTables.put(gen, diskTable) != null) {
            throw new IllegalStateException("table have already flushed!");
        }
        return new TableSet(this.memTable, tablesToFlush, newDiskTables, this.generation);
    }

    /**
     * Complete compaction.
     *
     * @param compactList compacted tables
     * @param dest        destination of compacted tables
     * @param gen         generation
     * @return new TableSet
     */
    @NotNull
    public TableSet afterCompact(
            @NotNull final NavigableMap<Integer, Table> compactList,
            @NotNull final Table dest,
            final int gen) {
        final NavigableMap<Integer, Table> newDiskTables =
                new TreeMap<>(this.diskTables);
        for (final var entry : compactList.entrySet()) {
            if (!newDiskTables.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("compactList is not part of diskTables");
            }
        }
        if (newDiskTables.put(gen, dest) != null) {
            throw new IllegalStateException("Error in compaction");
        }
        return new TableSet(new MemTable(this.generation), this.flushingTables, newDiskTables, this.generation);
    }

    /**
     * Mark as compact start.
     *
     * @return new TableSet
     */
    @NotNull
    public TableSet beforeCompact() {
        return
                new TableSet(
                        memTable,
                        flushingTables,
                        diskTables,
                        this.generation + 1);
    }
}
