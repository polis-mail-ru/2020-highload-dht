package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ThreadSafe
public final class SortedStringTable implements Table {
    private final int rows;
    private final int size;

    private final FileChannel fileChannel;

    @Override
    public long getSize() {
        return
                (long) size + (rows + 1) * Integer.BYTES;
    }

    @NotNull
    @Override
    public Iterator<TableCell> iterator(
            @NotNull final ByteBuffer point) throws IOException {
        return new Iterator<>() {
            int next = findNext(point);

            @Override
            public boolean hasNext() {
                return next < rows;
            }

            /**
             * Пользователь дергает next.
             * Движемся по итераторам - мерджим их,
             * выбираем самое свежее значение, движемся дальше.
             *
             * @return самое свежее значение
             */
            @Override
            public TableCell next() {
                assert hasNext();
                return findCell(next++);
            }
        };
    }

    private TableCell findCell(
            final int index) {
        assert index >= 0 && index < rows;

        int offset = 0;
        try {
            offset = findOffset(index);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Ex in getting offset 1: {}", ioException);
        }
        final ByteBuffer keySize =
                ByteBuffer.allocate(Integer.BYTES);

        try {
            fileChannel.read(keySize, offset);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Ex in read file channel 1: {}", ioException);
        }

        offset += Integer.BYTES;
        final int key = keySize.rewind().getInt();
        final ByteBuffer keyBuf =
                ByteBuffer.allocate(key);

        try {
            fileChannel.read(keyBuf, offset);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Ex in read file channel 2: {}", ioException);
        }

        offset += key;
        final ByteBuffer timeStampBuf =
                ByteBuffer.allocate(Long.BYTES);

        try {
            fileChannel.read(timeStampBuf, offset);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Ex in read file channel 3: {}", ioException);
        }

        final long timeStamp = timeStampBuf.rewind().getLong();

        if (timeStamp < 0) {
            return new TableCell(keyBuf.rewind(), new Value(-timeStamp));
        } else {
            offset += Long.BYTES;

            final int fullSize;
            if (index == rows - 1) {
                fullSize = size - offset;
            } else {
                try {
                    fullSize = findOffset(index + 1) - offset;
                } catch (IOException ioException) {
                    throw new UncheckedIOException("Ex in getting offset 2: {}", ioException);
                }
            }

            final ByteBuffer data =
                    ByteBuffer.allocate(fullSize);

            try {
                fileChannel.read(data, offset);
            } catch (IOException ioException) {
                throw new UncheckedIOException("Ex in read file channel 4: {}", ioException);
            }

            return new TableCell(keyBuf.rewind(), new Value(
                    timeStamp,
                    data.rewind()));
        }
    }

    // бинарный поиск поверх файла
    private int findNext(
            final ByteBuffer point) throws IOException {
        int l = 0;
        int r = rows - 1;
        while (l < r + 1) {
            // берем строчку n/2
            final int m = l + (r - l) / 2;
            // прыгаем по этой строке,
            // читаем ключ, сравниваем с тем, что пользователь передал
            final int cmp = findK(m).compareTo(point);
            // понимаем в какую сторону смотреть
            if (cmp < 0) {
                l = m + 1;
            } else if (cmp > 0) {
                r = m - 1;
            } else {
                return m;
            }
        }
        return l;
    }

    private ByteBuffer findK(
            final int index) throws IOException {
        assert 0 <= index && index < rows;

        final int offset =
                findOffset(index);
        final ByteBuffer keySize =
                ByteBuffer.allocate(Integer.BYTES);

        fileChannel.read(
                keySize,
                offset);

        final int key =
                keySize
                        .rewind()
                        .getInt();
        final ByteBuffer keyBuf =
                ByteBuffer.allocate(key);

        fileChannel.read(
                keyBuf,
                offset + Integer.BYTES);

        return keyBuf.rewind();
    }

    private int findOffset(
            final int num) throws IOException {
        final ByteBuffer offsetBuf =
                ByteBuffer.allocate(Integer.BYTES);

        fileChannel.read(
                offsetBuf,
                size + num * Integer.BYTES);

        return offsetBuf
                .rewind()
                .getInt();
    }

    /**
     * Отсортированная таблица на диске.
     * После записи на диск поддерживает только операции чтения.
     *
     * @param f файл
     * @throws IOException если ошибка при работе с файлом
     */
    public SortedStringTable(
            @NotNull final File f) throws IOException {
        fileChannel =
                FileChannel.open(
                        f.toPath(),
                        StandardOpenOption.READ);

        final int fSize =
                (int) fileChannel.size() - Integer.BYTES;
        final ByteBuffer tableCellsAmount =
                ByteBuffer.allocate(Integer.BYTES);

        fileChannel.read(
                tableCellsAmount,
                fSize);

        rows =
                tableCellsAmount.rewind().getInt();
        size =
                fSize - rows * Integer.BYTES;
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(
            @NotNull final ByteBuffer key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Запись таблицы на диск.
     *
     * @param cells итератор по ячейкам таблицы
     * @param target файл, куда пишем
     * @throws IOException если ошибка при работе с файлом
     */
    public static void writeData(
            final Iterator<TableCell> cells,
            final File target) throws IOException {
        try (FileChannel fileChannel = new FileOutputStream(target).getChannel()) {
            final List<Integer> offsets =
                    new ArrayList<>();
            int offset = 0;
            while (cells.hasNext()) {
                final TableCell tableCell =
                        cells.next();
                final ByteBuffer key =
                        tableCell.getKey();

                offsets.add(offset);
                offset += key.remaining() + Long.BYTES + Integer.BYTES;
                fileChannel.write(
                        ByteBuffer
                                .allocate(Integer.BYTES)
                                .putInt(key.remaining())
                                .rewind());
                fileChannel.write(key);

                /*
                TimeStamp Module
                храним монотонно увеличивающийся в системе Time Stamp,
                чтобы можно было взять строки и по значению версии определить что свежее
                 */
                final Value val =
                        tableCell.getVal();
                if (val.wasRemoved()) {
                    fileChannel.write(
                            ByteBuffer
                                    .allocate(Long.BYTES)
                                    .putLong(-tableCell.getVal().getTimeStamp())
                                    .rewind());
                } else {
                    fileChannel.write(
                            ByteBuffer
                                    .allocate(Long.BYTES)
                                    .putLong(tableCell.getVal().getTimeStamp())
                                    .rewind());
                    final ByteBuffer data = tableCell.getVal().getData();
                    offset += data.remaining();
                    fileChannel.write(data);
                }
            }

            for (final Integer o : offsets) {
                fileChannel.write(
                        ByteBuffer
                                .allocate(Integer.BYTES)
                                .putInt(o)
                                .rewind());
            }

            fileChannel.write(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .putInt(offsets.size())
                            .rewind());
        }
    }
}
