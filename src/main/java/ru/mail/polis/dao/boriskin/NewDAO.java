package ru.mail.polis.dao.boriskin;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.Iters;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

/**
 * Своя реализация {@link NewDAO} интерфейса {@link DAO}.
 *
 * @author Makary Boriskin
 */
public final class NewDAO implements DAO {

    private static final Logger log = LoggerFactory.getLogger(NewDAO.class);

    private final int maxSSTableCollectionThreshold;

    private final File base;
    private final long maxHeapThreshold;

    private Table memTable;
    private final Collection<SortedStringTable> ssTableCollection;

    // счетчик поколений
    private int gen;

    private static final String NAME = "SortedStringTABLE";
    private static final String DB = ".db";
    private static final String TEMP = ".tmp";

    /**
     * Конструктор {link NewDAO} instance.
     *
     * @param base папка диска, где хранятся данные
     * @param maxHeapThreshold порог, согласно которому судим когда сбросить таблицу на диск
     * @throws IOException обработка получения на вход не того base
     */
    public NewDAO(final File base, final long maxHeapThreshold) throws IOException {
        this.base = base;
        assert maxHeapThreshold >= 0L;
        this.maxHeapThreshold = maxHeapThreshold;

        this.maxSSTableCollectionThreshold = 64;

        memTable = new MemTable();
        ssTableCollection = new ArrayList<>();

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
        Files.walkFileTree(base.toPath(),
                new SimpleFileVisitor<>() {
           @Override
           public FileVisitResult visitFile(final Path path,
                                            final BasicFileAttributes attributes) throws IOException {
               if (path.toFile().isFile() && path.getFileName().toString().endsWith(DB)
                       && path.getFileName().toString().startsWith(NAME)) {
                   ssTableCollection.add(new SortedStringTable(path.toFile()));
                   // более свежая версия из того, что лежит на диске
                   gen = Math.max(gen, getGeneration(path.toFile().getName()));
               }

               return CONTINUE;
           }
        });
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull final ByteBuffer point) throws IOException {

        // после мерджа ячеек разных таблиц,
        // при возвращении итератора пользователю:
        // в этот момент превращает их в рекорды (transform)
        return Iterators.transform(iterateThroughTableCells(point),
                cell -> Record.of(cell.getKey(), cell.getValue().getData()));
    }

    private Iterator<TableCell> iterateThroughTableCells(@NotNull final ByteBuffer point) throws IOException {
        final Collection<Iterator<TableCell>> filesIterator = new ArrayList<>();

        for (final SortedStringTable sortedStringTable : ssTableCollection) {
            filesIterator.add(sortedStringTable.iterator(point));
        }

        filesIterator.add(memTable.iterator(point));
        // итератор мерджит разные потоки и выбирает самое актуальное значение
        final Iterator<TableCell> cells = Iters.collapseEquals(
                Iterators.mergeSorted(filesIterator, TableCell.COMPARATOR),
                TableCell::getKey);
        // может быть "живое" значение, а может быть, что значение по ключу удалили в момент времени Time Stamp
        return Iterators.filter(cells,
                cell -> !cell.getValue().wasRemoved());
    }

    // вставить-обновить
    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer val) throws IOException {
        memTable.upsert(key, val);
        // когда размер таблицы достигает порога,
        // сбрасываем данную таблицу на диск,
        // где она хранится в бинарном сериализованном виде
        if (memTable.getSize() >= maxHeapThreshold) {
            flush();
        }

        if (ssTableCollection.size() > maxSSTableCollectionThreshold) {
            compact();
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        memTable.remove(key);
        // сбрасываем таблицу на диск
        if (memTable.getSize() >= maxHeapThreshold) {
            flush();
        }

        if (ssTableCollection.size() > maxSSTableCollectionThreshold) {
            compact();
        }
    }

    @Override
    public void close() throws IOException {
        // сохранить все, что мы не сохранили
        if (memTable.getSize() > 0) {
            flush();
        }
    }

    private void flush() throws IOException {
        // в начале нужно писать во временный файл
        final File temp = new File(base, NAME + gen + TEMP);

        try {
            SortedStringTable.writeData(
                    memTable.iterator(ByteBuffer.allocate(0)),
                    temp);
        } catch (IOException ex) {
            Files.delete(temp.toPath());
            throwDBStrangeBehaviour();
        }

        // превращаем в постоянный файл
        final File dest = new File(base, NAME + gen + DB);
        Files.move(temp.toPath(), dest.toPath(), ATOMIC_MOVE);

        ssTableCollection.add(new SortedStringTable(dest));

        // обновляем счетчик поколений
        gen++;
        // заменяем MemTable в памяти на пустой
        memTable = new MemTable();
        // таким образом, на диске копятся SSTable'ы + есть пустой-непустой MemTable в памяти
    }

    private void throwDBStrangeBehaviour() throws IOException {
        throw new IOException("БД в странном состоянии");
    }

    private int getGeneration(final String name) {
        for (int i = 0; i < Math.min(9, name.length()); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return i == 0 ? 0 : Integer.parseInt(name.substring(0, i));
            }
        }
        return -1;
    }

    @Override
    public void compact() throws IOException {
        gen = 1;
        final File temp = new File(base, NAME + gen + TEMP);

        try {
            SortedStringTable.writeData(
                    iterateThroughTableCells(ByteBuffer.allocate(0)),
                    temp);
        } catch (IOException ex) {
            Files.delete(temp.toPath());
            throwDBStrangeBehaviour();
        }

        for (final SortedStringTable sortedStringTable : ssTableCollection) {
            try {
                Files.delete(sortedStringTable.getTable().toPath());
            } catch (IOException ex) {
                log.warn("Не удалось удалить: " + ex);
            }
        }

        ssTableCollection.clear();

        final File dest = new File(base, NAME + gen + DB);
        Files.move(temp.toPath(), dest.toPath(), ATOMIC_MOVE);
        ssTableCollection.add(new SortedStringTable(dest));

        gen = ssTableCollection.size() + 1;
    }
}
