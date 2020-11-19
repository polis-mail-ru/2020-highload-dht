package ru.mail.polis.service.suhova;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static ru.mail.polis.service.suhova.DAOServiceMethods.toByteArray;

public class StreamSession extends HttpSession {
    static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    static final byte[] END = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private Iterator<Record> iterator;

    /**
     * Implementation of Session.
     *
     * @param socket - socket
     * @param server - server
     */
    public StreamSession(@NotNull final Socket socket,
                         @NotNull final HttpServer server) {
        super(socket, server);
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();
        if (iterator != null) {
            next();
        }
    }

    /**
     * Send streaming response.
     *
     * @param iterator - iterator from DAO
     */
    public void setIterator(final Iterator<Record> iterator) throws IOException {
        if (iterator == null) {
            throw new IOException("Iterator is null!");
        }
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        this.iterator = iterator;
        writeResponse(response, false);
        next();
    }

    private synchronized void next() throws IOException {
        byte[] data;
        while (iterator.hasNext() && queueHead == null) {
            data = toChunk(iterator.next());
            write(data, 0, data.length);
        }
        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }
        if (!iterator.hasNext()) {
            write(END, 0, END.length);
            server.incRequestsProcessed();
            final String connection = handling.getHeader("Connection: ");
            final boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
            if (!keepAlive) scheduleClose();
            this.handling = handling = pipeline.pollFirst();
            if (handling != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    server.handleRequest(handling, this);
                }
            }
        }
    }

    private byte[] toChunk(final Record record) {
        final byte[] key = toByteArray(record.getKey());
        final byte[] value = toByteArray(record.getValue());
        final byte[] bbRecord = toByteArray(
            ByteBuffer.allocate(key.length + LF.length + value.length)
                .put(key).put(LF).put(value).position(0)
        );
        final byte[] len = Integer.toHexString(bbRecord.length)
            .getBytes(StandardCharsets.UTF_8);
        return toByteArray(
            ByteBuffer.allocate(bbRecord.length + 2 * CRLF.length + len.length)
                .put(len).put(CRLF).put(bbRecord).put(CRLF).position(0)
        );
    }
}
