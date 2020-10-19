package ru.mail.polis.dao.basta123;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class RecordIter implements Iterator<Record> {

    private final RocksIterator rocksIterator;

    public RecordIter(final RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        if (!hasNext()) {
            throw new IllegalStateException("next doesn't exist");
        }
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
