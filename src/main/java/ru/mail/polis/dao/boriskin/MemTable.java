package ru.mail.polis.dao.boriskin;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class MemTable implements Table {
    private final NavigableMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();
    private final AtomicLong size = new AtomicLong();

    @Override
    public long getSize() {
        return size.get();
    }

    @NotNull
    @Override
    public Iterator<TableCell> iterator(
            @NotNull final ByteBuffer point) {
        return Iterators.transform(
                map.tailMap(point).entrySet().iterator(),
                e -> new TableCell(e.getKey(), e.getValue()));
    }

    @Override
    public void upsert(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer val) {
        map.put(
                key.duplicate(),
                new Value(
                        System.currentTimeMillis(),
                        val.duplicate())
        );
        size.addAndGet(
                key.remaining() + val.remaining() + Long.BYTES);
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
    public void remove(
            @NotNull final ByteBuffer key) {
        if (map.containsKey(key)) {
            if (!map.get(key).wasRemoved()) {
                size.addAndGet(
                        -map
                                .get(key)
                                .getData()
                                .remaining()
                );
            }
        } else {
            size.addAndGet(
                    key.remaining() + Long.BYTES);
        }
        map.put(
                key,
                new Value(System.currentTimeMillis()));
    }

}
