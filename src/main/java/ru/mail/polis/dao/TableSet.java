package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.MemTable;
import ru.mail.polis.SSTable;
import ru.mail.polis.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TableSet {

    @NotNull
    public final NavigableMap<Integer, Table> ssTableCollection;
    @NotNull
    public final Set<Table> tablesReadyToFlush;
    @NotNull
    public final MemTable currMemTable;
    public int generation;

    /**
     * Внутренний конструктор исплючительно для TableSet.
     *
     * @oaram memTable - memTable
     * @param flushing - flushing set
     * @param ssTables - ssTable
     * @param generation - generation
     */
    private TableSet(
            @NotNull final MemTable memTable,
            @NotNull final Set<Table> flushing,
            @NotNull final NavigableMap<Integer, Table> ssTables,
            final int generation) {
        assert generation >= 0;
        this.ssTableCollection = ssTables;
        this.tablesReadyToFlush = Collections.unmodifiableSet(flushing);
        this.currMemTable = memTable;
        this.generation = generation;
    }

    /**
     * Конструктор TableSet для DAO.
     *
     * @param ssTables - ssTable
     * @param generation - generation
     */
    public TableSet(
            @NotNull final NavigableMap<Integer, Table> ssTables,
            final int generation) {
        assert generation >= 0;
        this.ssTableCollection = ssTables;
        this.tablesReadyToFlush = new HashSet<>();
        this.currMemTable = new MemTable();
        this.generation = generation;
    }

    /**
     * Flush from memTable to flushing.
     *
     * @param flushing - flushing hash set Table
     * @return TableSet
     */
    @NotNull
    public TableSet fromMemTableToFlushing(@NotNull final Set<Table> flushing) {
        final Set<Table> flush = new HashSet<>(flushing);
        flush.add(currMemTable);
        return new TableSet(new MemTable(), flush, ssTableCollection, ++generation);
    }

    /**
     * Flush from flushing to SSTable.
     *
     * @param deleteMemTable - delete mem table
     * @param flushing - flushing hash set Table
     * @param ssTable - ssTable
     * @return TableSet
     */
    @NotNull
    public TableSet fromFlushingToSSTable(
            @NotNull final MemTable deleteMemTable,
            @NotNull final Set<Table> flushing,
            @NotNull final SSTable ssTable) {
        final Set<Table> flush = new HashSet<>(flushing);
        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTableCollection);
        if (flush.remove(deleteMemTable)) {
            files.put(generation, ssTable);
        }
        return new TableSet(currMemTable, flush, files, generation);
    }

    /**
     * Compact.
     *
     * @param memTable - memTable
     * @param ssTable - ssTable
     * @return TableSet
     */
    @NotNull
    public TableSet compact(@NotNull final MemTable memTable, @NotNull final SSTable ssTable) {
        final NavigableMap<Integer, Table> files = new TreeMap<>();
        files.put(generation, ssTable);
        generation = 1;
        return new TableSet(memTable, tablesReadyToFlush, files, generation);
    }
}
