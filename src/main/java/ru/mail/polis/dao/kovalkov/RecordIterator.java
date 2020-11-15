package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.nio.ByteBuffer;
import java.util.Iterator;

public final class RecordIterator implements Iterator<Record>{

    private final RocksIterator rocksIterator;

    RecordIterator(@NotNull final RocksIterator iterator) {
        this.rocksIterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() throws IllegalStateException {
        if (!hasNext())throw new IllegalStateException("No further");
        final ByteBuffer bufferKey = BufferConverter.foldToBuffer(rocksIterator.key());
        final ByteBuffer bufferValue = ByteBuffer.wrap(rocksIterator.value());
        final Record record = Record.of(bufferKey, bufferValue);
        rocksIterator.next();
        return record;
    }

    public void close() {
        rocksIterator.close();
    }
}
