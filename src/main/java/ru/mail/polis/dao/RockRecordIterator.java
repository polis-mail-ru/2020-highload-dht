package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class RockRecordIterator implements Iterator<Record>, AutoCloseable {
    private final RocksIterator iterator;

    RockRecordIterator(@NotNull final RocksIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.isValid();
    }

    @Override
    public Record next() throws IllegalStateException {
        if (!hasNext()) {
            throw new IllegalStateException("Empty");
        }

        final var key = iterator.key();
        final var value = iterator.value();
        iterator.next();
        return Record.of(ByteBuffer.wrap(key), ByteBuffer.wrap(value));
    }

    @Override
    public void close() {
        iterator.close();
    }
}
