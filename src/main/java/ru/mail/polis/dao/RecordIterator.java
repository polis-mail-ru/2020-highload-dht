package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.service.ivanovandrey.Util;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RecordIterator implements Iterator<Record>, AutoCloseable {

    private final RocksIterator iterator;

    RecordIterator(@NotNull final RocksIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.isValid();
    }

    @Override
    public Record next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final var key = iterator.key();
        final var value = iterator.value();
        iterator.next();
        return Record.of(Util.fromArrayShifted(key), ByteBuffer.wrap(value));
    }

    @Override
    public void close() {
        iterator.close();
    }
}
