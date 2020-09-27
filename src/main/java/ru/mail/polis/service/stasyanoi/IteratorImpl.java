package ru.mail.polis.service.stasyanoi;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class IteratorImpl implements Iterator<Record> {

    private final RocksIterator rocksIterator;

    public IteratorImpl(final ByteBuffer key, final RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
        this.rocksIterator.seek(CustomServer.toBytes(key));
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        final byte[] key = rocksIterator.key();
        final byte[] value = rocksIterator.value();
        rocksIterator.next();
        return Record.of(CustomServer.fromBytes(key), CustomServer.fromBytes(value));
    }
}
