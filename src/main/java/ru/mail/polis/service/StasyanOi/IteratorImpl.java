package ru.mail.polis.service.StasyanOi;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class IteratorImpl implements Iterator<Record> {

    private RocksIterator rocksIterator;

    public IteratorImpl(RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        rocksIterator.next();
        byte[] key = rocksIterator.key();
        byte[] value = rocksIterator.value();
        return Record.of(ByteBuffer.wrap(key), ByteBuffer.wrap(value));
    }
}
