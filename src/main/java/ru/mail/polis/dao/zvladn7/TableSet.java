package ru.mail.polis.dao.zvladn7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

final class TableSet {

    private static final Logger log = LoggerFactory.getLogger(TableSet.class);

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
        final Set<MemoryTable> newMemToFlush = new HashSet<>(this.memToFlush);
        newMemToFlush.add(this.memTable);
        return new TableSet(new MemoryTable(), newMemToFlush, ssTables, generation + 1);
    }

    TableSet finishFlushingOnDisk(final MemoryTable flushedMemTable,
                                  final File dst,
                                  final int generation) throws IOException {
        final Set<MemoryTable> newMemToFlush = new HashSet<>(this.memToFlush);
        final boolean isRemoved = newMemToFlush.remove(flushedMemTable);
        if (!isRemoved) {
            throw new IOException("Failed to flush memory table on disk!");
        }
        final NavigableMap<Integer, Table> newSsTables = new TreeMap<>(this.ssTables);
        newSsTables.put(generation, new SSTable(dst));
        log.debug("File " + dst.getName() + " was flushed");
        return new TableSet(memTable, newMemToFlush, newSsTables, this.generation);
    }

    TableSet startCompact() {
        return new TableSet(memTable, memToFlush, ssTables, generation + 1);
    }

    TableSet finishCompact(final NavigableMap<Integer, Table> compactedSSTables,
                           final File dst,
                           final int generation) throws IOException {
        final NavigableMap<Integer, Table> newSSTables = new TreeMap<>(ssTables);
        final boolean containsAll = ssTables.entrySet().containsAll(compactedSSTables.entrySet());
        if (containsAll) {
            newSSTables.entrySet().removeAll(compactedSSTables.entrySet());
        } else {
            throw new IllegalStateException("Files to compact were lost!");
        }

        log.debug("File " + dst.getName() + " was compacted");
        if (newSSTables.put(generation, new SSTable(dst)) != null) {
            throw new IllegalStateException("File already exist on compaction");
        }

        return new TableSet(memTable, memToFlush, newSSTables, this.generation);

    }
}
