package ru.mail.polis.service.codearound;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class RocksRecordIterator implements Iterator<Record> {

    final RocksIterator rocksIt;

    RocksRecordIterator(final RocksIterator rocksIt) {
        this.rocksIt = rocksIt;
    }

    /**
     * define iterator move to next element of stuff within file content
     */
    @Override
    public boolean hasNext() {
        return rocksIt.isValid();
    }

    /**
     * define evaluation of an element (record) at next step of file reading by iterator
     */
    @Override
    public Record next() {
        if(hasNext()) {
            final ByteBuffer buf = DAOByteOnlyConverter.tuneArrayToBuf(rocksIt.key());
            final Record rec = Record.of(buf, ByteBuffer.wrap(rocksIt.value()));
            rocksIt.next();
            return rec;
        } else {
            throw new IllegalStateException("End of file content reached. Iterator stopped\n");
        }
    }

    /**
     * define termination of iterator call
     */
    public void close() {
        rocksIt.close();
    }
}
