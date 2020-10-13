package ru.mail.polis.dao.valaubr;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class MemTable implements Table {
    /*
     * ByteBuffer [link - 2 bytes
     * + bytes in the buffer - n bytes
     * + headline - 8 bytes
     * + int inside 4 bytes
     * + boolean inside - 1 bytes] Total: n + 15 bytes (about)
     *
     * Value [headline 8 bytes
     * + ByteBuffer - n + 15 bytes
     * + long - 8 bytes
     * + link - 2 bytes] Total: n + 31 bytes (about)
     *
     * SortedMap [link - 2 bytes
     * + headline - 8 bytes
     * + for each object Key + Value
     * + 4 bytes links in each object
     * + 8 bytes on the headline of each node]
     *
     * Total: n + n + 68 (about when inserting a new object)
     */
    //NEW_VALUE_ADDITIONAL_SIZE
    private static final int NVAS = 68;
    //OLD_VALUE_ADDITIONAL_SIZE
    private static final int OVAS = 39;
    //KEY_ADDITIONAL_SIZE
    private static final int KAS = 15;
    private final SortedMap<ByteBuffer, Value> map = new ConcurrentSkipListMap<>();

    private final AtomicLong sizeInBytes = new AtomicLong();

    public MemTable() {
        sizeInBytes.set(0);
    }

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull final ByteBuffer from) {
        return map.tailMap(from)
                .entrySet()
                .stream()
                .map(entry -> new Cell(entry.getKey(), entry.getValue()))
                .iterator();
    }

    @Override
    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value) {
        final Value val = map.put(key, new Value(System.currentTimeMillis(), value.duplicate()));
        if (val == null) {
            sizeInBytes.addAndGet(key.remaining() + value.remaining() + NVAS);
        } else {
            sizeInBytes.addAndGet(value.remaining() + OVAS);
        }
    }

    @Override
    public void remove(@NotNull final ByteBuffer key) {
        final Value val = map.put(key, new Value(System.currentTimeMillis()));
        if (val == null) {
            sizeInBytes.addAndGet(key.remaining() + KAS);
        } else if (!val.isTombstone()) {
            sizeInBytes.addAndGet(-val.getData().remaining());
        }
    }

    @Override
    public long getSizeInBytes() {
        return sizeInBytes.get();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void close() {
        map.clear();
        sizeInBytes.set(0);
    }
}
