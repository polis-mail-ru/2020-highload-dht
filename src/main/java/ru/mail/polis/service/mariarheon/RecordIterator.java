package ru.mail.polis.service.mariarheon;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class RecordIterator implements Iterator<ru.mail.polis.Record> {
    private final Iterator<ru.mail.polis.Record> iterator;
    private final byte[] endKeyAsBytes;
    private boolean finished;
    private ru.mail.polis.Record current;

    public RecordIterator(DAO dao, String start, String end) throws IOException {
        final var startKey = ByteBuffer.wrap(start.getBytes(StandardCharsets.UTF_8));
        iterator = dao.iterator(startKey);
        if (end != null) {
            endKeyAsBytes = end.getBytes(StandardCharsets.UTF_8);
        } else {
            endKeyAsBytes = null;
        }
        finished = false;
        readNext();
    }

    private void readNext() {
        if (finished || !iterator.hasNext()) {
            finished = true;
            current = null;
            return;
        }
        current = iterator.next();
        final var key = current.getKey();
        final var keyAsBytes = ByteBufferUtils.toArray(key);
        if (endKeyAsBytes != null && Util.compare(keyAsBytes, endKeyAsBytes) >= 0) {
            finished = true;
            current = null;
        }
    }

    @Override
    public boolean hasNext() {
        return !finished;
    }

    @Override
    public ru.mail.polis.Record next() {
        final var res = current;
        readNext();
        return res;
    }
}
