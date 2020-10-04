package ru.mail.polis.dao.zvladn7;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

class TableSet {

    final MemoryTable memTable;
    final Set<MemoryTable> memToFlush;
    final NavigableMap<Integer, Table> ssTables;
    final int generation;

    private TableSet(
            final MemoryTable memTable,
            final Set<MemoryTable> memToFlush,
            final NavigableMap<Integer, Table> ssTables,
            final int generation) {
        this.memTable = memTable;
        this.memToFlush = memToFlush;
        this.ssTables = ssTables;
        this.generation = generation;
    }

    static TableSet provideTableSet(final NavigableMap<Integer, Table> ssTables, final int generation) {
        return new TableSet(new MemoryTable(), new HashSet<>(), ssTables, generation);
    }

    TableSet startFlushingOnDisk() {
        final Set<MemoryTable> memToFlush = new HashSet<>(this.memToFlush);
        memToFlush.add(this.memTable);
        return new TableSet(new MemoryTable(), memToFlush, ssTables, generation + 1);
    }

    TableSet finishFlushingOnDisk(final MemoryTable flushedMemTable, final File dst) throws IOException {
        final Set<MemoryTable> memToFlush = new HashSet<>(this.memToFlush);
        boolean isRemoved = memToFlush.remove(flushedMemTable);
        if (!isRemoved) {
            throw new IOException("Failed to flush memory table on disk!");
        }
        final NavigableMap<Integer, Table> newSsTables = new ConcurrentSkipListMap<>(this.ssTables);
        newSsTables.put(generation, new SSTable(dst));
        return new TableSet(memTable, memToFlush, newSsTables, generation);
    }

    TableSet startCompact() {
        return new TableSet(memTable, memToFlush, ssTables, generation + 1);
    }

    TableSet finishCompact(final NavigableMap<Integer, Table> compactedSSTables, final File dst) throws IOException {
        final NavigableMap<Integer, Table> newSSTables = new ConcurrentSkipListMap<>(ssTables);
        boolean containsAll = ssTables.entrySet().containsAll(compactedSSTables.entrySet());
        if (containsAll) {
            newSSTables.entrySet().removeAll(compactedSSTables.entrySet());
        } else {
            throw new IllegalStateException("Files to compact were lost!");
        }
        newSSTables.put(generation, new SSTable(dst));

        return new TableSet(memTable, memToFlush, newSSTables, generation);

    }
}
