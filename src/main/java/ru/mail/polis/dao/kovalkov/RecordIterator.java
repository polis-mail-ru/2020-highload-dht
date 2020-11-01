package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.nio.ByteBuffer;
import java.util.Iterator;

public final class RecordIterator implements Iterator<Record>, AutoCloseable {

    private final RocksIterator rocksIterator;

    RecordIterator(@NotNull final RocksIterator iterator) {
        this.rocksIterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    @NotNull
    public Record next() throws IllegalStateException {
        if (!hasNext())throw new IllegalStateException("No further");
        final byte[] bytesKey = rocksIterator.key();
        final ByteBuffer bufferKey = BufferConverter.foldToBuffer(bytesKey);
        final byte[] bytesValue = rocksIterator.value();
        final ByteBuffer bufferValue = ByteBuffer.wrap(bytesValue);
        final Record record = Record.of(bufferKey, bufferValue);
        rocksIterator.next();
        return record;
    }

    @Override
    public void close() {
        rocksIterator.close();
    }
}
