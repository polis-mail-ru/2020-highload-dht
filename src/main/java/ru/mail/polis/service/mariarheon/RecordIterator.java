package ru.mail.polis.service.mariarheon;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Iterator for getting records for keys in range [start, end).
 */
public class RecordIterator implements Iterator<ru.mail.polis.Record> {
    private final Iterator<ru.mail.polis.Record> iterator;
    private final byte[] endKeyAsBytes;
    private boolean finished;
    private ru.mail.polis.Record current;

    /**
     * Create iterator for getting records for keys in range [start, end).
     *
     * @param dao - dao implementation with records
     * @param start - start-key of range, inclusively.
     * @param end - end-key of range, exclusively, or null if all
     *            the records should be delivered, started from start-key.
     * @throws IOException - raised when reading from dao failed.
     */
    public RecordIterator(@NotNull final DAO dao, @NotNull final String start, String end) throws IOException {
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

    /**
     * Returns true if the next record exists.
     *
     * @return - true if the next record exists, false - otherwise.
     */
    @Override
    public boolean hasNext() {
        return !finished;
    }

    /**
     * Returns the next record or null if it does not exist.
     *
     * @return - the next record or null if it does not exist.
     */
    @Override
    public ru.mail.polis.Record next() {
        final var res = current;
        readNext();
        return res;
    }
}
