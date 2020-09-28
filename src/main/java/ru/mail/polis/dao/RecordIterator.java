package ru.mail.polis.dao;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class RecordIterator implements Iterator<Record>, AutoCloseable{
    private final RocksIterator rocksIterator;

    public RecordIterator(RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        if(!hasNext()){
            throw new RuntimeException();
        }
        Record record = Record.of(ByteBuffer.wrap(rocksIterator.key()),
                    ByteBuffer.wrap(rocksIterator.value()));
        rocksIterator.next();
        return record;
    }

    @Override
    public void close() {
        rocksIterator.close();
    }
}
