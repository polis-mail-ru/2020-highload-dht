package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.MemTable;
import ru.mail.polis.SSTable;
import ru.mail.polis.Table;

import java.util.*;

public class TableSet {

    @NotNull
    public final NavigableMap<Integer, Table> ssTableCollection;
    @NotNull
    public final Set<Table> tablesReadyToFlush;
    @NotNull
    public final MemTable currMemTable;
    public int generation;

    public TableSet(@NotNull final MemTable memTable,
                    @NotNull final Set<Table> flushing,
                    @NotNull final NavigableMap<Integer, Table> ssTables,
                    final int generation) {
        assert generation >= 0;
        this.ssTableCollection = ssTables;
        this.tablesReadyToFlush = Collections.unmodifiableSet(flushing);
        this.currMemTable = memTable;
        this.generation = generation;
    }

    public TableSet(@NotNull final NavigableMap<Integer, Table> ssTables,
                    final int generation) {
        assert generation >= 0;
        this.ssTableCollection = ssTables;
        this.tablesReadyToFlush = new HashSet<>();
        this.currMemTable = new MemTable();
        this.generation = generation;
    }

    @NotNull
    public TableSet fromMemTableToFlushing(@NotNull final Set<Table> flushing) {
        final Set<Table> flush = new HashSet<>(flushing);
        flush.add(currMemTable);
        return new TableSet(new MemTable(), flush, ssTableCollection, ++generation);
    }

    @NotNull
    public TableSet fromFlushingToSSTable(@NotNull final MemTable deleteMem,
                                   @NotNull final Set<Table> flushing,
                                   @NotNull final SSTable ssTable) {
        final Set<Table> flush = new HashSet<>(flushing);
        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTableCollection);
        if (flush.remove(deleteMem)) {
            files.put(generation, ssTable);
        }
        return new TableSet(currMemTable, flush, files, generation);
    }

    @NotNull
    public TableSet compact(@NotNull final MemTable memTable, @NotNull final SSTable sstable) {
        final NavigableMap<Integer, Table> files = new TreeMap<>();
        files.put(generation, sstable);
        generation = 1;
        return new TableSet(memTable, tablesReadyToFlush, files, generation);
    }
}
