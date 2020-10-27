package ru.mail.polis.dao.boriskin;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Своя реализация {@link NewDAO} интерфейса {@link DAO}.
 *
 * @author Makary Boriskin
 */
public class NewDAO implements DAO {
    @NotNull
    private final File base;
    private final long maxHeapThreshold;

    private static final String DB = ".db";
    private static final String TEMP = ".tmp";

    private final ReentrantReadWriteLock readWriteLock =
            new ReentrantReadWriteLock();
    // набор всех таблиц, с которыми мы работаем
    @NotNull
    @GuardedBy("readWriteLock")
    private TableSet tables;

    /**
     * Конструктор {link NewDAO} instance.
     *
     * @param base папка диска, где хранятся данные
     * @param maxHeapThreshold порог, согласно которому судим когда сбросить таблицу на диск
     * @throws IOException обработка получения на вход не того base
     */
    public NewDAO(
            @NotNull final File base,
            final long maxHeapThreshold) throws IOException {
        this.base = base;
        assert maxHeapThreshold >= 0L;
        this.maxHeapThreshold = maxHeapThreshold;
        final NavigableMap<Long, Table> ssTableCollection =
                new TreeMap<>();
        final AtomicLong identifierThreshold =
                new AtomicLong();

        /*
          Сканируем иерархию в папке с целью понять, что там лежат SSTable'ы.
          Пример, в общем случае:
          Структура дерева
          /
          -usr/
          --local/
          ---goto-> (symlink to /etc/share)
          -etc/
          --share/
          ---sample.db
          ---goto-> (symlink to /usr)

          Пользователь задает папку для поиска /usr/local
          1. Ты не используешь обход ссылок.
          Тогда файл .db просто не найдется.

          2. Используешь обход ссылок, но ставишь ограничение глубины (что-то порядка 100 должно быть).
          Тогда поиск попадет на симлинк ведущий в /etc/share/,
          если повезет, то следующим узлом для обхода будет как раз нужный .db файлик.
          Но так как порядок обхода дерева не гарантирован, то можно попасть на ссылку в /usr
          и тогда обход зациклится.
          Как только лимит по глубине в 100 будет достигнут, sample.db будет найден.

          Однако в данном конкретном случае, можно обойтись без FOLLOW_LINKS.
         */
        try (Stream<Path> stream = Files.walk(base.toPath(), 1)) {
            stream.filter(path -> {
                final String fName = path.getFileName().toString();
                return fName.endsWith(DB)
                        && !path.toFile().isDirectory()
                        && fName.substring(0, fName.indexOf(DB)).matches("^[0-9]+$"); })
                    .forEach(path -> {
                            final String fName =
                                    path.getFileName().toString();
                            final long gen =
                                    Integer.parseInt(fName.substring(0, fName.indexOf(DB)));
                            // более свежая версия из того, что лежит на диске
                            identifierThreshold.set(
                                    Math.max(
                                            identifierThreshold.get(),
                                            gen));
                        try {
                            ssTableCollection.put(
                                    gen,
                                    new SortedStringTable(path.toFile()));
                        } catch (IOException ioException) {
                            throw new UncheckedIOException("Ex while put in ssTableCollection: {}", ioException);
                        }
                    });
        }
        identifierThreshold.set(
                identifierThreshold.get() + 1);
        this.tables =
                new TableSet(
                        new MemTable(),
                        Collections.emptySet(),
                        ssTableCollection,
                        identifierThreshold.get());
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer point) throws IOException {
        // после мерджа ячеек разных таблиц,
        // при возвращении итератора пользователю:
        // в этот момент превращает их в рекорды (transform)
        return Iterators.transform(iterateThroughTableCells(point),
                cell -> Record.of(cell.getKey(), cell.getVal().getData()));
    }

    /**
     * Определяет итератор по "живым" ячейкам таблицы.
     *
     * @param point где определен интератор
     * @return итератор по "живым" ячейкам таблицы
     * @throws IOException в случае ошибки в доступе к итератору таблицы
     */
    @NotNull
    public Iterator<TableCell> iterateThroughTableCells(
            @NotNull final ByteBuffer point) throws IOException {
        final TableSet snapshot;
        readWriteLock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            readWriteLock.readLock().unlock();
        }
        final List<Iterator<TableCell>> iteratorList = new ArrayList<>(
                snapshot.ssTableCollection.size() + 1);
        iteratorList.add(
                snapshot
                        .currMemTable
                        .iterator(point));

        for (final Table table : snapshot.tablesReadyToFlush) {
            iteratorList.add(table.iterator(point));
        }
        // итератор мерджит разные потоки и выбирает самое актуальное значение
        final Iterator<TableCell> alive =
                returnIteratorOverMergedCollapsedFiltered(
                        snapshot,
                        point,
                        iteratorList);
        // может быть "живое" значение, а может быть, что значение по ключу удалили в момент времени Time Stamp
        return Iterators.filter(
                alive, cell -> !cell.getVal().wasRemoved());
    }

    /**
     * Возвращает ячейку по ключу.
     *
     * @param key ключ
     * @return ячейка; если не найдена, то null
     * @throws IOException если ошибка ввод-вывод
     */
    @Nullable
    public TableCell getTableCell(@NotNull final ByteBuffer key) throws IOException {
        final Iterator<TableCell> iter = iterateThroughTableCells(key);
        if (!iter.hasNext()) {
            return null;
        }
        final TableCell tableCell = iter.next();
        if (!tableCell.getKey().equals(key)) {
            return null;
        }
        return tableCell;
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer val) throws IOException {
        final boolean flushPending;
        readWriteLock.readLock().lock();
        try {
            tables.currMemTable.upsert(key, val);
            flushPending =
                    tables.currMemTable.getSize() >= maxHeapThreshold;

        } finally {
            readWriteLock.readLock().unlock();
        }
        // когда размер таблицы достигает порога,
        // сбрасываем данную таблицу на диск,
        // где она хранится в бинарном сериализованном виде
        if (flushPending) {
            flush();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        final boolean flushPending;
        readWriteLock.readLock().lock();
        try {
            tables.currMemTable.remove(key);
            flushPending =
                    tables.currMemTable.getSize() >= maxHeapThreshold;

        } finally {
            readWriteLock.readLock().unlock();
        }
        // сбрасываем таблицу на диск
        if (flushPending) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        // сохранить все, что мы не сохранили
        flush();
    }

    private void flush() throws IOException {
        final TableSet snapshot;
        readWriteLock.writeLock().lock();
        try {
            snapshot = this.tables;
            if (snapshot.currMemTable.getSize() == 0L) {
                return;
            }
            this.tables = snapshot.setToFlush();
        } finally {
            readWriteLock.writeLock().unlock();
        }
        // в начале нужно писать во временный файл
        final File temp =
                new File(base, snapshot.gen + TEMP);
        SortedStringTable.writeData(
                snapshot
                        .currMemTable
                        .iterator(ByteBuffer.allocate(0)),
                temp);
        // превращаем в постоянный файл
        final File dest =
                new File(base, snapshot.gen + DB);
        Files.move(
                temp.toPath(),
                dest.toPath(),
                StandardCopyOption.ATOMIC_MOVE);
        readWriteLock.writeLock().lock();
        try {
            this.tables =
                    this.tables
                            .flushTable(
                                    snapshot.currMemTable,
                                    new SortedStringTable(dest),
                                    snapshot.gen);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public synchronized void compact() throws IOException {
        final TableSet snapshot;
        readWriteLock.readLock().lock();
        try {
            snapshot = this.tables;
        } finally {
            readWriteLock.readLock().unlock();
        }
        final ByteBuffer point =
                ByteBuffer.allocate(0);
        final List<Iterator<TableCell>> fileIterators =
                new ArrayList<>(
                        snapshot.ssTableCollection.size());
        final Iterator<TableCell> cells =
                returnIteratorOverMergedCollapsedFiltered(snapshot, point, fileIterators);

        readWriteLock.writeLock().lock();
        try {
            this.tables = this.tables.compactSSTables();
        } finally {
            readWriteLock.writeLock().unlock();
        }
        final File temp = new File(base, snapshot.gen + TEMP);
        SortedStringTable.writeData(cells, temp);
        final File dest = new File(base, snapshot.gen + DB);
        Files.move(
                temp.toPath(),
                dest.toPath(),
                StandardCopyOption.ATOMIC_MOVE);

        readWriteLock.writeLock().lock();
        try {
            this.tables =
                    this.tables
                            .flushCompactTable(
                                    snapshot.ssTableCollection,
                                    new SortedStringTable(dest),
                                    snapshot.gen);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        for (final long gen : snapshot.ssTableCollection.keySet()) {
            final File f = new File(base, gen + DB);
            Files.delete(f.toPath());
        }
    }

    private Iterator<TableCell> returnIteratorOverMergedCollapsedFiltered(
            final TableSet snapshot,
            final ByteBuffer point,
            final List<Iterator<TableCell>> iteratorList) throws IOException {
        for (final Table table : snapshot.ssTableCollection.descendingMap().values()) {
            iteratorList.add(table.iterator(point));
        }
        final Iterator<TableCell> merged =
                Iterators.mergeSorted(iteratorList, Comparator.naturalOrder());

        return Iters.collapseEquals(merged, TableCell::getKey);
    }
}
