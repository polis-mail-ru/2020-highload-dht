package ru.mail.polis.dao.basta123;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.service.basta123.MyHTTPServer;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class MyRecordIter implements Iterator<Record> {

    private RocksIterator rocksIterator;

    public MyRecordIter(ByteBuffer key, RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
        this.rocksIterator.seek(getByteArrayFromByteBuffer(key));
    }

    @Override
    public boolean hasNext() {
        return rocksIterator.isValid();
    }

    @Override
    public Record next() {
        byte[] keyBytes = rocksIterator.key();
        byte[] valueBytes = rocksIterator.value();
        rocksIterator.next();

        ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);
        ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueBytes);

        Record record = Record.of(keyByteBuffer, valueByteBuffer);
        return record;
    }

    public void close() {
        rocksIterator.close();
    }
}
