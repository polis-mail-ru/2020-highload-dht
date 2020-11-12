package ru.mail.polis.service.ivanovandrey;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChunkedIterator implements Iterator<Record>, AutoCloseable {

    private final RocksIterator iterator;
    private int i = 0;
    private final long count;

    public ChunkedIterator(@NotNull final RocksIterator iterator, long count) {
        this.iterator = iterator;
        this.count = count;
    }

    @Override
    public boolean hasNext() {
        return (iterator.isValid() && i < count);
    }

    @Override
    public Record next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final var key = iterator.key();
        final var value = iterator.value();
        iterator.next();
        this.i++;
        return Record.of(Util.fromArrayShifted(key), ByteBuffer.wrap(value));
    }

    @Override
    public void close() {
        iterator.close();
    }
}
