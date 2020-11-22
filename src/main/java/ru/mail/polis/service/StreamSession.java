package ru.mail.polis.service;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StreamSession extends HttpSession {
    private static final byte[] LF_BYTES = "\n".getBytes(UTF_8);
    private static final byte[] CRLF_BYTES = "\r\n".getBytes(UTF_8);
    private static final byte[] EMPTY_BUFFER_BYTES = "0\r\n\r\n".getBytes(UTF_8);
    private static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding: chunked";

    private Iterator<Record> iterator;

    StreamSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    void setIterator(final Iterator<Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader(TRANSFER_ENCODING_HEADER);
        writeResponse(response, false);
        init();
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();
        init();
    }

    private void init() throws IOException {
        if (iterator == null) {
            return;
        }

        byte[] data;
        while (iterator.hasNext() && queueHead == null) {
            final Record rawData = iterator.next();
            final byte[] key = Util.toByteArray(rawData.getKey());
            final byte[] value = Value.composeFromBytes(Util.toByteArray(rawData.getValue())).getBytes();
            data = formChunkWithData(key, value);
            write(data, 0, data.length);
        }

        if (iterator.hasNext()) {
            return;
        }

        write(EMPTY_BUFFER_BYTES, 0, EMPTY_BUFFER_BYTES.length);

        Request requestHandling = handling;
        if (requestHandling == null) {
            throw new IOException("Out of order");
        }
        server.incRequestsProcessed();
        final String connection = requestHandling.getHeader("Connection: ");
        final boolean keepAlive = requestHandling.isHttp11()
                ? !"Close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);

        if (!keepAlive) {
            scheduleClose();
        }

        handling = requestHandling = pipeline.pollFirst();
        if (requestHandling != null) {
            if (requestHandling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(requestHandling, this);
            }
        }
    }

    private byte[] formChunkWithData(final byte[] key, final byte[] value) {
        final int bufferSize = key.length + LF_BYTES.length + value.length;
        final byte[] hexLength = Integer.toHexString(bufferSize).getBytes(UTF_8);
        final byte[] bytes = new byte[bufferSize + 2 * CRLF_BYTES.length + hexLength.length];

        return ByteBuffer.wrap(bytes)
                .put(hexLength)
                .put(CRLF_BYTES)
                .put(key)
                .put(LF_BYTES)
                .put(value)
                .put(CRLF_BYTES)
                .array();
    }

}
