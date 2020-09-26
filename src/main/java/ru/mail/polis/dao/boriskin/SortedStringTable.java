package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SortedStringTable implements Table {
    private final long size;
    private final int rows;

    private final File table;

    // хранит указатели на начало каждой строки
    private final IntBuffer offsets;
    private final ByteBuffer cells;

    File getTable() {
        return table;
    }

    @Override
    public long getSize() {
        return size;
    }

    @NotNull
    @Override
    public Iterator<TableCell> iterator(@NotNull final ByteBuffer point) throws IOException {
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

    private TableCell findCell(final int index) {
        assert index >= 0 && index < rows;

        int offset = offsets.get(index);

        // используем длину ключа
        final int sizeOfK = cells.getInt(offset);
        offset += Integer.BYTES;

        final ByteBuffer key = cells.duplicate();
        key.position(offset);
        key.limit(key.position() + sizeOfK);
        offset += sizeOfK;

        // работа с версией
        final long timeStamp = cells.getLong(offset);
        offset += Long.BYTES;

        if (timeStamp < 0) {
            // если это могилка, то дальше ничего нет
            return new TableCell(key.slice(), new Value(-timeStamp, null));
        } else {
            // Values Module
            final int sizeOfV = cells.getInt(offset);
            offset += Integer.BYTES;

            final ByteBuffer val = cells.duplicate();
            val.position(offset);
            val.limit(val.position() + sizeOfV);

            // если это нормальное значение, то дальше длина этого значения и само значение
            return new TableCell(key.slice(), new Value(timeStamp, val.slice()));
        }
    }

    // бинарный поиск поверх файла
    private int findNext(final ByteBuffer point) {
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

    private Comparable<ByteBuffer> findK(final int index) {
        assert 0 <= index && index < rows;

        final int offset = offsets.get(index);
        final int sizeOfK = cells.getInt(offset);
        final ByteBuffer key = cells.duplicate();

        key.position(offset + Integer.BYTES);
        key.limit(key.position() + sizeOfK);

        return key.slice();
    }

    // Отсортированная таблица на диске.
    // После записи на диск поддерживает только операции чтения.
    SortedStringTable(@NotNull final File f) throws IOException {
        this.table = f;
        this.size = f.length();
        assert size != 0 && size <= Integer.MAX_VALUE;

        final MappedByteBuffer mapped;
        try (FileChannel fileChannel = FileChannel.open(f.toPath(), StandardOpenOption.READ)) {
            mapped = (MappedByteBuffer) fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, 0L, fileChannel.size()
            ).order(ByteOrder.BIG_ENDIAN);
        }

        rows = mapped.getInt((int) (size - Integer.BYTES));

        final ByteBuffer offsetsByteBuffer = mapped.duplicate();
        final ByteBuffer cellsByteBuffer = mapped.duplicate();

        offsetsByteBuffer.position(mapped.limit() - Integer.BYTES * rows - Integer.BYTES);
        offsetsByteBuffer.limit(mapped.limit() - Integer.BYTES);
        cellsByteBuffer.limit(offsetsByteBuffer.position());

        this.offsets = offsetsByteBuffer.slice().asIntBuffer();
        this.cells = cellsByteBuffer.slice();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer val) throws IOException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) throws IOException {
        throw new UnsupportedOperationException("");
    }

    static void writeData(final Iterator<TableCell> cells, final File target) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(target.toPath(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            final List<Integer> offsets = new ArrayList<>();
            int offset = 0;
            while (cells.hasNext()) {
                offsets.add(offset);

                final TableCell tableCell = cells.next();

                final ByteBuffer key = tableCell.getKey();
                final int sizeOfK = key.remaining();

                fileChannel.write(Bytes.fromInt(sizeOfK));
                offset += Integer.BYTES;
                fileChannel.write(key);
                offset += sizeOfK;

                final Value val = tableCell.getValue();

                /*
                TimeStamp Module
                храним монотонно увеличивающийся в системе Time Stamp,
                чтобы можно было взять строки и по значению версии определить что свежее
                 */
                if (val.wasRemoved()) {
                    fileChannel.write(Bytes.fromLong(-tableCell.getValue().getTimeStamp()));
                } else {
                    fileChannel.write(Bytes.fromLong(tableCell.getValue().getTimeStamp()));
                }

                offset += Long.BYTES;

                if (!val.wasRemoved()) {
                    final ByteBuffer data = val.getData();
                    final int sizeOfV = data.remaining();

                    fileChannel.write(Bytes.fromInt(sizeOfV));
                    offset += Integer.BYTES;
                    fileChannel.write(data);
                    offset += sizeOfV;
                }
            }

            for (final Integer o : offsets) {
                fileChannel.write(Bytes.fromInt(o));
            }

            fileChannel.write(Bytes.fromInt(offsets.size()));
        }
    }
}
