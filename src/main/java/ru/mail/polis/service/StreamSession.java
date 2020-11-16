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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class StreamSession extends HttpSession {
    private Iterator<Record> iterator;

    StreamSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    void setIterator(final Iterator<Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
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
            final byte[] value = Util.toByteArray(rawData.getValue());
            data = formChunkWithData(key, value);
            write(data, 0, data.length);
        }

        if (iterator.hasNext()) {
            return;
        }

        data = formChunk(new byte[0]);
        write(data, 0, data.length);

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
        final byte[] lf = "\n".getBytes(StandardCharsets.UTF_8);
        final int bufferSize = key.length + lf.length + value.length;
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.put(key).put(lf).put(value).position(0);
        return formChunk(Util.toByteArray(buffer));
    }

    private byte[] formChunk(final byte[] data) {
        final byte[] crlfBytes = "\r\n".getBytes(StandardCharsets.UTF_8);
        final byte[] hexLength = Integer.toHexString(data.length)
                .getBytes(StandardCharsets.US_ASCII);
        final int chunkLength = data.length + 2 * crlfBytes.length + hexLength.length;
        final ByteBuffer chunk = ByteBuffer.allocate(chunkLength);
        chunk.put(hexLength).put(crlfBytes).put(data).put(crlfBytes).position(0);
        return Util.toByteArray(chunk);
    }
}
