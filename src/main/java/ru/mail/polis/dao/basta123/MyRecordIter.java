package ru.mail.polis.dao.basta123;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class MyRecordIter implements Iterator<Record> {

    private final RocksIterator rocksIterator;

    public MyRecordIter(final RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        final byte[] keyBytes = rocksIterator.key();
        final byte[] valueBytes = rocksIterator.value();
        rocksIterator.next();

        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);
        final ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueBytes);

        return Record.of(keyByteBuffer, valueByteBuffer);
    }

    public void close() {
        rocksIterator.close();
    }
}
