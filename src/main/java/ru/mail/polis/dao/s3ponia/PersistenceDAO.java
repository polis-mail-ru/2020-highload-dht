package ru.mail.polis.dao.s3ponia;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

public final class PersistenceDAO implements DAO {
    private final DiskManager manager;
    private final Table currTable;
    private final long maxMemory;
    private long currMemory;
    private static final long MIN_FREE_MEMORY = 128 * 1024 * 1024 / 32;

    private PersistenceDAO(final File data, final long maxMemory) throws IOException {
        this.manager = new DiskManager(Paths.get(data.getAbsolutePath(),
                DiskManager.META_PREFIX + data.getName() + DiskManager.META_EXTENSION));
        this.currTable = new Table(manager.getGeneration());
        this.maxMemory = maxMemory;
    }

    private void flush() throws IOException {
        manager.save(currTable);
        currMemory = 0;
        currTable.close();
    }

    private void checkToFlush() throws IOException {
        if (maxMemory - currMemory < MIN_FREE_MEMORY && currTable.size() > 0) {
            flush();
        }
    }

    public static PersistenceDAO of(final File data, final long memorySize) throws IOException {
        return new PersistenceDAO(data, memorySize);
    }

    /**
     * Return iterator over all existing records.
     * @return Iterator over all records
     */
    @NotNull
    public Iterator<Record> iterator() {
        final var diskTables = manager.diskTables();
        final var diskIterators = new ArrayList<Iterator<Table.ICell>>();
        diskIterators.add(currTable.iterator());
        diskTables.forEach(diskTable -> diskIterators.add(diskTable.iterator()));
        final var merge = Iterators.mergeSorted(diskIterators, Table.ICell::compareTo);
        final var newest = Iters.collapseEquals(merge, Table.ICell::getKey);
        final var removeDead = Iterators.filter(newest, el -> !el.getValue().isDead());

        return Iterators.transform(removeDead, c -> Record.of(c.getKey(), c.getValue().getValue()));
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final var diskTables = manager.diskTables();
        final var diskIterators = new ArrayList<Iterator<Table.ICell>>();
        diskIterators.add(currTable.iterator(from));
        diskTables.forEach(diskTable -> diskIterators.add(diskTable.iterator(from)));
        final var merge = Iterators.mergeSorted(diskIterators, Table.ICell::compareTo);
        final var newest = Iters.collapseEquals(merge, Table.ICell::getKey);
        final var removeDead = Iterators.filter(newest, el -> !el.getValue().isDead());

        return Iterators.transform(removeDead, c -> Record.of(c.getKey(), c.getValue().getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        currMemory += key.limit() + value.limit() + Long.BYTES + Integer.BYTES;
        checkToFlush();
        if (currMemory != 0) {
            currMemory -= key.limit() + value.limit() + Long.BYTES + Integer.BYTES;
        }
        currTable.upsert(key, value);
        currMemory += key.limit() + value.limit() + Long.BYTES + Integer.BYTES;

    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        currMemory += key.limit() + Long.BYTES + Integer.BYTES;
        checkToFlush();
        if (currMemory != 0) {
            currMemory -= key.limit() + Long.BYTES + Integer.BYTES;
        }
        currTable.remove(key);
        currMemory += key.limit() + Long.BYTES + Integer.BYTES;
    }

    @Override
    public void close() throws IOException {
        if (currTable.size() > 0) {
            flush();
        }
    }

    @Override
    public void compact() throws IOException {
        close();
        final var it = iterator();
        final var diskTables = manager.diskTables();
        manager.clear();
        while (it.hasNext()) {
            final var record = it.next();
            upsert(record.getKey(), record.getValue());
        }
        for (final var disk : diskTables) {
            disk.erase();
        }
    }
}
