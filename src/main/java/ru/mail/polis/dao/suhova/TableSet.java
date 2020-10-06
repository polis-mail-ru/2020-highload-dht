package ru.mail.polis.dao.suhova;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TableSet {
    public final NavigableMap<Integer, Table> ssTables;
    public final Set<Table> flushing;
    public final MemTable memTable;
    public int generation;
    private static final Logger logger = LoggerFactory.getLogger(TurboDAO.class);

    /**
     * Set of all tables DAO (MemTable + Flushing tables + Files on disk).
     *
     * @param memTable   - memory table
     * @param flushing   - tables to be flushed to disk
     * @param ssTables   - files on disk
     * @param generation - generation this Memory Table
     */
    public TableSet(@NotNull final MemTable memTable,
                    @NotNull final Set<Table> flushing,
                    @NotNull final NavigableMap<Integer, Table> ssTables,
                    final int generation) {
        assert generation >= 0;
        this.ssTables = ssTables;
        this.flushing = flushing;
        this.memTable = memTable;
        this.generation = generation;
    }

    /**
     * Set of all tables DAO (MemTable + Flushing tables + Files on disk).
     *
     * @param ssTables   - files on disk
     * @param generation - generation this Memory Table
     */
    public TableSet(@NotNull final NavigableMap<Integer, Table> ssTables,
                    final int generation) {
        assert generation >= 0;
        this.ssTables = ssTables;
        this.flushing = new HashSet<>();
        this.memTable = new MemTable();
        this.generation = generation;
    }

    @NotNull
    TableSet fromMemTableToFlushing() {
        flushing.add(memTable);
        return new TableSet(new MemTable(), flushing, ssTables, ++generation);
    }

    @NotNull
    TableSet fromFlushingToSSTable(@NotNull final MemTable memTable, @NotNull final SSTable ssTable) {
        final NavigableMap<Integer, Table> files = new TreeMap<>(this.ssTables);
        if (ssTables.put(generation, ssTable) != null) {
            logger.error("Rewrite table with generation {}", generation);
        }
        if (!flushing.remove(memTable)) {
            logger.debug("Can't remove this table");
        }
        return new TableSet(this.memTable, flushing, files, generation);
    }

}
