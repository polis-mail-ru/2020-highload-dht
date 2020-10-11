package ru.mail.polis.dao.alexander.marashov;

import java.io.File;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

final public class TableSnapshot {

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

    /**
     * Initializing correct TableSnapshot state.
     * @param ssTables - tables that are already on the disk.
     * @param flushingTables - tables that are currently being flushed.
     * @param generation - max free generation for MemTable to start with.
     * @return TableSnapshot instance.
     */
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

    /**
     * Switches the TableSnapshot state according to the flushing logic.
     * @return new state of the TableSnapshot.
     */
    public TableSnapshot flushIntent() {
        final ConcurrentSkipListMap<Integer, Table> newFlushingTables = new ConcurrentSkipListMap<>(flushingTables);
        newFlushingTables.put(generation, memTable);
        return new TableSnapshot(
                new MemTable(),
                generation + 1,
                storageTables,
                newFlushingTables
        );
    }

    /**
     * Switches the TableSnapshot state according to the logic of loading the table from the storage.
     * Deletes loaded table from the FlushingTables map.
     * @param oldGeneration - generation of the old MemTable to become SSTable.
     * @param file - where the data is located.
     * @return new state of the TableSnapshot.
     */
    public TableSnapshot loadTableIntent(
            final int oldGeneration,
            final File file
    ) {
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

    /**
     * Switches the TableSnapshot state according to the logic of tables compacting.
     * @param tablesToRemove - tables that were compacted.
     * @param compactedGen - table generation to become SSTable.
     * @param compactedFile - where the compacted data is located.
     * @return new state of the TableSnapshot.
     */
    public TableSnapshot compactIntent(
            final NavigableMap<Integer, Table> tablesToRemove,
            final int compactedGen,
            final File compactedFile
    ) {
        final ConcurrentSkipListMap<Integer, Table> newStorageTables = new ConcurrentSkipListMap<>(storageTables);
        for (final Integer integer : tablesToRemove.keySet()) {
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
