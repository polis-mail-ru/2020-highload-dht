package ru.mail.polis.dao;

import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.util.Util;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RecordIterator implements Iterator<Record> {

    private final RocksIterator iterator;

    RecordIterator(final RocksIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.isValid();
    }

    @Override
    public Record next() {
        if (hasNext()) {
            final ByteBuffer buf = Util.fromShiftedArray(iterator.key());
            final Record rec = Record.of(buf, ByteBuffer.wrap(iterator.value()));
            iterator.next();
            return rec;
        } else {
            throw new NoSuchElementException("Further record isn't found. Iterator stopped\n");
        }
    }

    /**
     * stops iterator running, closes file processed.
     */
    public void close() {
        iterator.close();
    }
}
