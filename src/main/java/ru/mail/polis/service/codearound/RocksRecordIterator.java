package ru.mail.polis.service.codearound;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RocksRecordIterator implements Iterator<Record> {

    final RocksIterator rocksIt;

    RocksRecordIterator(final RocksIterator rocksIt) {
        this.rocksIt = rocksIt;
    }

    /**
     * enables iterator move to next element of stuff within file content.
     *
     * @return true if current element followed by another else
     */
    @Override
    public boolean hasNext() {
        return rocksIt.isValid();
    }

    /**
     * performs evaluation of an element (record) at next step of file reading by iterator.
     *
     * @return Record instance
     */
    @Override
    public Record next() {
        if (hasNext()) {
            final ByteBuffer buf = DAOByteOnlyConverter.tuneArrayToBuf(rocksIt.key());
            final Record rec = Record.of(buf, ByteBuffer.wrap(rocksIt.value()));
            rocksIt.next();
            return rec;
        } else {
            throw new NoSuchElementException("Further record isn't found. Iterator stopped\n");
        }
    }

    /**
     * stops iterator running, closes file processed.
     */
    public void close() {
        rocksIt.close();
    }
}
