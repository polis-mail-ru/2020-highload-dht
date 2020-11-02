package ru.mail.polis.dao.nik27090;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class DAOImpl implements DAO {
    private static final String SUFFIX = ".dat";
    private static final String TEMP = ".tmp";

    private TableSet tableSet;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @NotNull
    private final File storage;
    private final long flushSize;

    /**
     * Creates key-value database.
     *
     * @param storage   data storage
     * @param flushSize MemTable size at which MemTable converted to SSTable
     */
    public DAOImpl(
            @NotNull final File storage,
            final long flushSize) throws IOException {
        this.storage = storage;
        this.flushSize = flushSize;
        assert flushSize > 0L;
        final NavigableMap<Integer, SSTable> ssTables = new TreeMap<>();
        final AtomicInteger generation = new AtomicInteger(-1);

        try (Stream<Path> paths = Files.list(storage.toPath())) {
            paths
                    .filter(path -> {
                        final String name = path.getFileName().toString();
                        return name.endsWith(SUFFIX)
                                && !path.toFile().isDirectory()
                                && name.substring(0, name.indexOf(SUFFIX)).matches("^[0-9]+$");
                    })
                    .forEach(element -> {
                        try {
                            final String name = element.getFileName().toString();
                            final int foundedGen = Integer.parseInt(name.substring(0, name.indexOf(SUFFIX)));
                            generation.set(Math.max(generation.get(), foundedGen));
                            ssTables.put(foundedGen, new SSTable(element.toFile()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        this.tableSet = TableSet.fromFiles(ssTables, generation.get() + 1);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer from) throws IOException {
        final ByteBuffer key = from.rewind().duplicate();
        final Iterator<Cell> alive = getAliveCells(key);

        return Iterators.transform(alive, el -> Record.of(el.getKey(), el.getValue().getContent().rewind()));
    }

    @Override
    public void compact() throws IOException {
        final Iterator<Cell> alive = getAliveCells(ByteBuffer.allocate(0));

        final TableSet snapshot;

        lock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.startCompacting();
        } finally {
            lock.writeLock().unlock();
        }

        final File file = new File(storage, snapshot.generation + TEMP);
        SSTable.serialize(file, alive);

        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.replaceCompactedFiles(snapshot.files, new SSTable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }

        for (int i = snapshot.generation - 1; i >= 0; i--) {
            final File delFile = new File(storage, i + SUFFIX);
            try {
                Files.delete(delFile.toPath());
            } catch (NoSuchFileException e) {
                break;
            }
        }
    }

    private Iterator<Cell> getAliveCells(@NotNull final ByteBuffer key) throws IOException {

        final TableSet snapshot;

        lock.readLock().lock();
        try {
            snapshot = this.tableSet;
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            final List<Iterator<Cell>> iterators = new ArrayList<>(snapshot.files.size() + 1);
            iterators.add(snapshot.mem.iterator(key));
            snapshot.files.descendingMap().values().forEach(ssTable -> {
                try {
                    iterators.add(ssTable.iterator(key));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            for (final Table flushing : snapshot.flushing) {
                iterators.add(flushing.iterator(key));
            }
            final Iterator<Cell> merged = Iterators.mergeSorted(iterators, Cell.COMPARATOR);
            final Iterator<Cell> fresh = Iters.collapseEquals(merged, Cell::getKey);
            return Iterators.filter(fresh, el -> el.getValue().getContent() != null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) throws IOException {
        final boolean doFlush;
        lock.readLock().lock();
        try {
            tableSet.mem.upsert(key.rewind().duplicate(), value.rewind().duplicate());
            doFlush = tableSet.mem.getSizeInBytes() > flushSize;
        } finally {
            lock.readLock().unlock();
        }
        if (doFlush) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean doFlush;
        lock.readLock().lock();
        try {
            tableSet.mem.remove(key.rewind().duplicate());
            doFlush = tableSet.mem.getSizeInBytes() > flushSize;
        } finally {
            lock.readLock().unlock();
        }
        if (doFlush) {
            flush();
        }
    }

    private void flush() {
        final TableSet snapshot;
        final File file;
        lock.writeLock().lock();
        try {
            snapshot = this.tableSet;
            if (snapshot.mem.size() == 0) {
                return;
            }
            this.tableSet = snapshot.markedAsFlushing();
            file = new File(storage, snapshot.generation + TEMP);
            SSTable.serialize(
                    file,
                    snapshot.mem.iterator(ByteBuffer.allocate(0)));
            atomicMoveTempToSuffix(file, snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void atomicMoveTempToSuffix(final File file, final TableSet snapshot) throws IOException {
        final File dst = new File(storage, snapshot.generation + SUFFIX);
        Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

        lock.writeLock().lock();
        try {
            this.tableSet = this.tableSet.flushed(snapshot.mem, new SSTable(dst), snapshot.generation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        final boolean doFlush;
        lock.readLock().lock();
        try {
            doFlush = tableSet.mem.getSizeInBytes() > 0;
        } finally {
            lock.readLock().unlock();
        }
        if (doFlush) {
            flush();
        }
        tableSet.files.values().forEach(SSTable::close);
    }

    @Override
    public Cell getCell(@NotNull final ByteBuffer key) throws IOException {
        final Iterator<Cell> iter = getIterator(key);
        if (!iter.hasNext()) {
            throw new NoSuchElementException("Cell not found");
        }

        final Cell next = iter.next();
        if (next.getKey().equals(key)) {
            return next;
        } else {
            throw new NoSuchElementException("Cell not found");
        }
    }

    private Iterator<Cell> getIterator(@NotNull final ByteBuffer from) throws IOException {
        final List<Iterator<Cell>> filesIterators = new ArrayList<>();

        for (final Table fileTable : tableSet.files.values()) {
            filesIterators.add(fileTable.iterator(from));
        }

        filesIterators.add(tableSet.mem.iterator(from));

        final Iterator<Cell> mergedCells = Iterators.mergeSorted(filesIterators,
                Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue));
        return Iters.collapseEquals(mergedCells, Cell::getKey);
    }
}
