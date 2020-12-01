package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DaoSnapshot;
import ru.mail.polis.dao.s3ponia.Table;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.session.StreamingSession;
import ru.mail.polis.util.MapIterator;
import ru.mail.polis.util.RangeIterator;
import ru.mail.polis.util.Utility;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;

public class DaoService implements Closeable, HttpEntitiesHandler {
    private final DAO dao;

    public DaoService(@NotNull final DAO dao) {
        this.dao = dao;
    }

    /**
     * Synchronous deleting from dao.
     *
     * @param key  key to delete
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
     *
     * @param key   Record's key
     * @param value Record's value
     * @param time  time of putting in dao
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
     *
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
        final Response resp;
        if (v.isDead()) {
            resp = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else {
            resp = Response.ok(Utility.fromByteBuffer(v.getValue()));
        }
        resp.addHeader(Utility.DEADFLAG_TIMESTAMP_HEADER + ": " + v.getDeadFlagTimeStamp());
        return resp;
    }
    
    /**
     * Merges existing table to dao.
     * @param table a {@link Table}
     */
    public void merge(@NotNull final Table table) {
        dao.merge(table);
    }
    
    @Override
    public void close() throws IOException {
        dao.close();
    }

    private Iterator<Record> range(@NotNull final ByteBuffer from,
                                   @NotNull final ByteBuffer to) throws IOException {
        return new RangeIterator<>(from(from), Record.of(to, ByteBuffer.allocate(0)));
    }

    private Iterator<Record> from(@NotNull final ByteBuffer from) throws IOException {
        return dao.iterator(from);
    }

    public DaoSnapshot snapshot() {
        return dao.snapshot();
    }
    
    public Path tempFile() {
        return dao.tempFile();
    }

    @Override
    public void entities(String start, String end, StreamingSession session) throws IOException {
        Iterator<StreamingValue> streamIterator;

        if (end == null) {
            streamIterator = new MapIterator<>(
                    from(Utility.byteBufferFromString(start)),
                    StreamingRecordValue::new);
        } else {
            streamIterator = new MapIterator<>(
                    range(Utility.byteBufferFromString(start),
                            Utility.byteBufferFromString(end)),
                    StreamingRecordValue::new);
        }

        session.stream(streamIterator);
    }
}
