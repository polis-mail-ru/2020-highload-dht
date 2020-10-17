package ru.mail.polis.dao.zvladn7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

final class TableSet {

    private static final Logger log = LoggerFactory.getLogger(TableSet.class);

    final MemoryTable memTable;
    final Set<MemoryTable> memToFlush;
    final NavigableMap<Integer, Table> ssTablesToCompact;
    final NavigableMap<Integer, Table> ssTables;
    final int generation;

    private TableSet(
            final MemoryTable memTable,
            final Set<MemoryTable> memToFlush,
            final NavigableMap<Integer, Table> ssTablesToCompact,
            final NavigableMap<Integer, Table> ssTables,
            final int generation) {
        this.memTable = memTable;
        this.memToFlush = memToFlush;
        this.ssTablesToCompact = ssTablesToCompact;
        this.ssTables = ssTables;
        this.generation = generation;
    }

    static TableSet provideTableSet(final NavigableMap<Integer, Table> ssTables, final int generation) {
        return new TableSet(new MemoryTable(), new HashSet<>(), new TreeMap<>(), ssTables, generation);
    }

    TableSet startFlushingOnDisk() {
        final Set<MemoryTable> newMemToFlush = new HashSet<>(this.memToFlush);
        newMemToFlush.add(this.memTable);
        return new TableSet(new MemoryTable(), newMemToFlush, ssTablesToCompact, ssTables, generation + 1);
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
        return new TableSet(memTable, newMemToFlush, ssTablesToCompact, newSsTables, this.generation);
    }

    TableSet startCompact(final NavigableMap<Integer, Table> newSSTablesToCompact,
                          final NavigableMap<Integer, Table> newSSTable) {
        return new TableSet(memTable, memToFlush, newSSTablesToCompact, newSSTable, generation);
    }



    TableSet finishCompact(final NavigableMap<Integer, Table> compactedSSTables,
                           final File dst,
                           final int gen,
                           final File storage) throws IOException {
        final NavigableMap<Integer, Table> newSSTablesToCompact = new TreeMap<>(ssTablesToCompact);
        final NavigableMap<Integer, Table> newSSTables = new TreeMap<>(ssTables);
        if (newSSTablesToCompact.entrySet().containsAll(compactedSSTables.entrySet())) {
            newSSTablesToCompact.entrySet().removeAll(compactedSSTables.entrySet());
        } else {
            throw new IllegalStateException("Files to compact were lost!");
        }
        final String newFileName = gen + LsmDAOImpl.SSTABLE_FILE_POSTFIX;
        Path path = dst.toPath();
        Files.move(path, path.resolveSibling(newFileName));
        if (newSSTables.put(gen, new SSTable(new File(storage, newFileName))) != null) {
            throw new IllegalStateException("File already exist on compaction");
        }

        return new TableSet(memTable, memToFlush, newSSTablesToCompact, newSSTables, this.generation);
    }

    TableSet startIterating() {
        final Set<MemoryTable> newMemsToFlush = new HashSet<>(this.memToFlush);
        final NavigableMap<Integer, Table> newSSTablesToCompact = new TreeMap<>(this.ssTablesToCompact);
        final NavigableMap<Integer, Table> newSSTables = new TreeMap<>(this.ssTables);
        return new TableSet(memTable, newMemsToFlush, newSSTablesToCompact, newSSTables, this.generation);
    }
}
