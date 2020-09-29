package ru.mail.polis.dao.mariarheon;

import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RocksIteratorAdapter implements Iterator<Record>, AutoCloseable {

    private final RocksIterator iter;

    public RocksIteratorAdapter(@NotNull final RocksIterator iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return iter.isValid();
    }

    @Override
    public Record next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("Iterator is not valid!");
        }

        final Record record = Record.of(ByteBufferUtils.toByteBuffer(iter.key()),
                                         ByteBufferUtils.toByteBuffer(iter.value()));
        iter.next();

        return record;
    }

    @Override
    public void close() {
        iter.close();
    }
}
