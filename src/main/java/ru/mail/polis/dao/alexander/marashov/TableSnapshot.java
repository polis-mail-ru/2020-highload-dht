package ru.mail.polis.dao.alexander.marashov;

import java.io.File;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TableSnapshot {

    // current table for recording data
    final Table memTable;
    // generation of the current memory table
    final int generation;
    // read-only memory tables
    final NavigableMap<Integer, Table> flushingTables;
    // generation -> read-only storage table
    final NavigableMap<Integer, Table> storageTables;

    private TableSnapshot(
            final Table memTable,
            final int generation,
            final NavigableMap<Integer, Table> ssTables,
            final NavigableMap<Integer, Table> flushingTables
    ) {
        this.memTable = memTable;
        this.generation = generation;
        this.flushingTables = flushingTables;
        this.storageTables = ssTables;
    }

    public static TableSnapshot initializeTables(
            final NavigableMap<Integer, Table> ssTables,
            final NavigableMap<Integer, Table> flushingTables,
            final int generation
    ) {
        return new TableSnapshot(
                new MemTable(),
                generation,
                ssTables,
                flushingTables
        );
    }

    public TableSnapshot flushIntent() {
        ConcurrentSkipListMap<Integer, Table> newFlushingTables = new ConcurrentSkipListMap<>(flushingTables);
        newFlushingTables.put(generation, memTable);
        return new TableSnapshot(
                new MemTable(),
                generation + 1,
                storageTables,
                newFlushingTables
        );
    }

    public TableSnapshot fakeFlushIntent() {
        return new TableSnapshot(
                memTable,
                generation + 1,
                storageTables,
                flushingTables
        );
    }

    public TableSnapshot loadTableIntent(final int oldGeneration, final File file) {
        final ConcurrentSkipListMap<Integer, Table> newStorageTables = new ConcurrentSkipListMap<>(storageTables);
        final ConcurrentSkipListMap<Integer, Table> newFlushingTables = new ConcurrentSkipListMap<>(flushingTables);
        newFlushingTables.remove(oldGeneration);
        newStorageTables.put(oldGeneration, new SSTable(file));
        return new TableSnapshot(
                memTable,
                generation,
                newStorageTables,
                newFlushingTables
        );
    }

    public TableSnapshot compactIntent(final NavigableMap<Integer, Table> tablesToRemove, final int compactedGen, final File compactedFile) {
        final ConcurrentSkipListMap<Integer, Table> newStorageTables = new ConcurrentSkipListMap<>(storageTables);
        for (Integer integer : tablesToRemove.keySet()) {
            newStorageTables.remove(integer);
        }
        newStorageTables.put(compactedGen, new SSTable(compactedFile));
        return new TableSnapshot(
                memTable,
                generation,
                newStorageTables,
                flushingTables
        );
    }
}
