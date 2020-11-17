package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.RangeIterator;
import ru.mail.polis.util.Utility;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class DaoService implements Closeable, EntitiesService {
    private final DAO dao;

    public DaoService(@NotNull final DAO dao) {
        this.dao = dao;
    }

    /**
     * Synchronous deleting from dao.
     * @param key key to delete
     * @param time time of deletion
     * @return {@link Response} on deletion
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#removeWithTimeStamp}
     */
    public Response delete(@NotNull final ByteBuffer key,
                           final long time) throws DaoOperationException {
        try {
            dao.removeWithTimeStamp(key, time);
        } catch (IOException e) {
            throw new DaoOperationException("Remove error", e);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    /**
     * Synchronous putting in dao.
     * @param key Record's key
     * @param value Record's value
     * @param time time of putting in dao
     * @return {@link Response} result of putting
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#upsertWithTimeStamp}
     */
    public Response put(@NotNull final ByteBuffer key,
                        @NotNull final ByteBuffer value,
                        final long time) throws DaoOperationException {
        try {
            dao.upsertWithTimeStamp(key, value, time);
        } catch (IOException e) {
            throw new DaoOperationException("Upsert error", e);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * Synchronous get from dao.
     * @param key Record's key
     * @return {@link Response} result of getting
     * @throws DaoOperationException throw on {@link IOException} in {@link DAO#getValue}
     */
    public Response get(@NotNull final ByteBuffer key) throws DaoOperationException {
        final Value v;
        try {
            v = dao.getValue(key);
        } catch (IOException e) {
            throw new DaoOperationException("Get error", e);
        }
        if (v.isDead()) {
            final var resp = new Response(Response.NOT_FOUND, Response.EMPTY);
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        } else {
            final var resp = Response.ok(Utility.fromByteBuffer(v.getValue()));
            resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
            return resp;
        }
    }

    @Override
    public void close() throws IOException {
        dao.close();
    }

    @Override
    public Iterator<Record> range(@NotNull ByteBuffer from, @NotNull ByteBuffer to) throws IOException {
        return new RangeIterator<>(from(from), Record.of(to, ByteBuffer.allocate(0)));
    }

    @Override
    public Iterator<Record> from(@NotNull ByteBuffer from) throws IOException {
        return dao.iterator(from);
    }
}
