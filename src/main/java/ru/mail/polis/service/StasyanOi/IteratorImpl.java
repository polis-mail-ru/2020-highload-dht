package ru.mail.polis.service.StasyanOi;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class IteratorImpl implements Iterator<Record> {

    private ByteBuffer key;
    private RocksIterator rocksIterator;

    public IteratorImpl(ByteBuffer key, RocksIterator rocksIterator) {
        this.key = key;
        this.rocksIterator = rocksIterator;
    }

    @Override
    public boolean hasNext() {
        rocksIterator.seek(CustomServer.toBytes(key));
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        byte[] key = rocksIterator.key();
        byte[] value = rocksIterator.value();
        return Record.of(CustomServer.fromBytes(key), CustomServer.fromBytes(value));
    }
}
