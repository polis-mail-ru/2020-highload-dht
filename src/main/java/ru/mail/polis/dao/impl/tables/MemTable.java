package ru.mail.polis.dao.impl.tables;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.models.Value;

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
    private final AtomicLong sizeInBytes = new AtomicLong();

    @Override
    public long sizeInBytes() {
        return sizeInBytes.get();
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return Iterators.transform(
                map.tailMap(from).entrySet().iterator(),
                e -> new Cell(Objects.requireNonNull(e).getKey().duplicate(),
                        e.getValue()));
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key,
                       @NotNull final ByteBuffer value,
                       final long seconds,
                       final int nanos) {
        map.put(key.duplicate(),
                new Value(System.currentTimeMillis(),
                        value.duplicate(),
                        seconds, nanos));
        sizeInBytes.addAndGet(key.remaining() + value.remaining() + 2 * Long.BYTES + Integer.BYTES);
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        if (map.containsKey(key)) {
            if (!map.get(key).isTombstone()) {
                sizeInBytes.addAndGet(-Objects.requireNonNull(map.get(key).getData()).remaining());
            }
        } else {
            sizeInBytes.addAndGet(key.remaining() + Long.BYTES);
        }
        map.put(key.duplicate(),
                new Value(System.currentTimeMillis()));
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

}
