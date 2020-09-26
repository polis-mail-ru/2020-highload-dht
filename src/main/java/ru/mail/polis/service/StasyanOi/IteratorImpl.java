package ru.mail.polis.service.StasyanOi;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class IteratorImpl implements Iterator<Record> {

    private RocksIterator rocksIterator;

    public IteratorImpl(ByteBuffer key, RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
        this.rocksIterator.seek(CustomServer.toBytes(key));
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        byte[] key = rocksIterator.key();
        byte[] value = rocksIterator.value();
        rocksIterator.next();
        return Record.of(CustomServer.fromBytes(key), CustomServer.fromBytes(value));
    }
}
