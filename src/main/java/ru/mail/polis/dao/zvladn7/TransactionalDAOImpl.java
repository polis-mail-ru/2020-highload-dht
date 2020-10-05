package ru.mail.polis.dao.zvladn7;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.Iters;

import java.nio.ByteBuffer;
import java.util.*;

final class TransactionalDAOImpl implements TransactionalDAO {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalDAOImpl.class);
    private static long nextId;

    private final Long id;
    private final MemoryTable memoryTable;
    private final LsmDAOImpl dao;

    /**
     * TransactionalDAO implementation.
     *
     * @param dao - DAO which has started transaction
     */
    TransactionalDAOImpl(@NotNull final LsmDAOImpl dao) {
        this.memoryTable = new MemoryTable();
        this.dao = dao;
        this.id = nextId++;
    }

    @Override
    public void commit() {
        memoryTable.iterator(ByteBuffer.allocate(0)).forEachRemaining(cell -> {
            if (cell.getValue().isTombstone()) {
                dao.remove(cell.getKey());
            } else {
                dao.upsert(cell.getKey(), cell.getValue().getData());

            }
        });
    }

    @Override
    public void rollback() {
        unlockKeys(id);
        memoryTable.clear();
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) {
        final TableSet snapshot;
        dao.readLock.lock();
        try {
            snapshot = dao.tableSet;
        } finally {
            dao.readLock.unlock();
        }
        final List<Iterator<Cell>> iters = new ArrayList<>();
        iters.add(memoryTable.iterator(from));
        dao.getAllCellItersList(from, iters, snapshot);

        final Iterator<Cell> mergedElements = Iterators.mergeSorted(
                iters,
                Cell.BY_KEY_AND_VALUE_CREATION_TIME_COMPARATOR
        );

        final Iterator<Cell> freshElements = Iters.collapseEquals(mergedElements, Cell::getKey);
        final Iterator<Cell> aliveElements = Iterators.filter(freshElements, el -> !el.getValue().isTombstone());

        return Iterators.transform(aliveElements, el -> Record.of(el.getKey(), el.getValue().getData()));
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull final ByteBuffer key) {
        lock(key);

        final Iterator<Record> iter = iterator(key);
        if (!iter.hasNext()) {
            throw new NoSuchElementException("Record wasn't found");
        }

        final Record next = iter.next();
        if (next.getKey().equals(key)) {
            return next.getValue();
        } else {
            throw new NoSuchElementException("Record wasn't found");
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        lock(key);
        memoryTable.upsert(key, value);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        lock(key);
        memoryTable.remove(key);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close() aren't supported for TransactionalDAO class");
    }

    private void lock(@NotNull final ByteBuffer key) {
        final Long lockId = dao.lockTable.putIfAbsent(key, id);
        if (lockId != null && !id.equals(lockId)) {
            rollback();
            logger.warn("Transaction with id: {} was rolled back!", id);
            throw new ConcurrentModificationException("The key has been already locked by another transaction!");
        }
    }

    private void unlockKeys(@NotNull final Long id) {
        dao.lockTable.entrySet().removeIf(entry -> entry.getValue().equals(id));
    }
}
