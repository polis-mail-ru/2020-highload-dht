package ru.mail.polis.dao.boriskin;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public final class MemTable implements Table {

    private final SortedMap<ByteBuffer, Value> map = new TreeMap<>();
    private long size;

    @Override
    public long getSize() {
        return size;
    }

    @NotNull
    @Override
    public Iterator<TableCell> iterator(@NotNull final ByteBuffer point) throws IOException {
        return Iterators.transform(
          map.tailMap(point).entrySet().iterator(),
          e -> new TableCell(e.getKey(), e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer val) throws IOException {
        final Value prev = map.put(key, Value.valueOf(val));
        if (prev == null) {
            size += key.remaining() + val.remaining();
        } else if (prev.wasRemoved()) {
            size += val.remaining();
        } else {
            size += val.remaining() - prev.getData().remaining();
        }
    }

    /**
     * Когда клиент запрашивает данные по ключу,
     * мы также мерджим значения из всех SSTable'ов
     * и по Time Stamp в remove понимаем, что данная могилка - самая свежая
     * (самое последнее значение ключа - что он удален).
     * Пользователь получает - нет такого ключа.
     *
     * @param key передаваемый ключ
     */
    @Override
    public void remove(@NotNull final ByteBuffer key) {
        // сохраняем могилку (говорим, что значение removed)
        final Value prev = map.put(key, Value.tombstone());
        if (prev == null) {
            size += key.remaining();
        } else if (!prev.wasRemoved()) {
            size -= prev.getData().remaining();
        }
    }
}
