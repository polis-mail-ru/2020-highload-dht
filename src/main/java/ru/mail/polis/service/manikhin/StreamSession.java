package ru.mail.polis.service.manikhin;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class StreamSession extends HttpSession {
    private Iterator<Record> iterator;
    private static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] END_CHUNK_DATA = new byte[0];

    /**
     * Config StreamSession.
     */
    public StreamSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Set iterator, form 200-OK response and start sending chunks.
     */
    public void setIterator(final Iterator<Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response,false);
        stream();
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();

        if (iterator != null) {
            stream();
        }
    }

    private void stream() throws IOException {
        byte[] data;
        while (iterator.hasNext() && queueHead == null) {
            final Record record = iterator.next();

            data = formFilledChunk(record.getKey(), record.getValue());
            write(data,0,data.length);
        }

        if (!iterator.hasNext()) {
            data = formEndChunk();
            write(data,0,data.length);

            Request handling = this.handling;
            if (handling == null) {
                throw new IOException("Out of order response");
            }
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

    private byte[] formEndChunk() {
        return formChunk(END_CHUNK_DATA);
    }

    private byte[] formFilledChunk(final ByteBuffer key, final ByteBuffer value) {
        final int dataLength = key.limit() + LF.length + value.limit();
        final ByteBuffer data = ByteBuffer.wrap(new byte[dataLength]);
        data.put(key);
        data.put(LF);
        data.put(value);
        data.position(0);
        return formChunk(data.array());
    }

    private byte[] formChunk(final byte[] data) {
        final byte[] hexLength = Integer.toHexString(data.length)
                .getBytes(StandardCharsets.US_ASCII);
        final int chunkLength = data.length + 2 * CRLF.length + hexLength.length;
        final ByteBuffer chunk = ByteBuffer.wrap(new byte[chunkLength]);

        chunk.put(hexLength);
        chunk.put(CRLF);
        chunk.put(data);
        chunk.put(CRLF);
        chunk.position(0);
        return chunk.array();
    }
}
