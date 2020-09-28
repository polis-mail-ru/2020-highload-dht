package ru.mail.polis.service.codearound;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import java.nio.ByteBuffer;
import java.util.Iterator;
public class RocksRecordIterator implements Iterator<Record> {
    final private RocksIterator it;
    public RocksRecordIterator(final RocksIterator it) {
        this.it = it;
    }
    @Override
    public boolean hasNext() {
        return it.isValid();
    }

    @Override
    public Record next() {
        if (hasNext()) {
            final ByteBuffer buf = DAOByteOnlyConverter.tuneArrayToBuf(it.key());
            final Record rec = Record.of(buf, ByteBuffer.wrap(it.value()));
            it.next();
            return rec;
        } else {
            throw new IllegalStateException("End of file");
        }
    }

    public void close() {
        it.close();
    }
}