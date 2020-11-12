package ru.mail.polis.service.dariagap;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;
import ru.mail.polis.dao.dariagap.Timestamp;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class StreamSession extends HttpSession {
    private Iterator<Record> iterator;

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
            final byte[] key = Util.byteBufferToBytes(record.getKey());
            final byte[] value = Timestamp.getTimestampByData(record.getValue())
                    .getData();
            data = formFilledChunk(key,value);
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
            if ((this.handling = handling = pipeline.pollFirst()) != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    server.handleRequest(handling, this);
                }
            }
        }
    }

    private byte[] formEndChunk() {
        return formChunk(new byte[0]);
    }

    private byte[] formFilledChunk(final byte[] key, final byte[] value) {
        final byte[] lf = "\n".getBytes(StandardCharsets.UTF_8);
        final int dataLength = key.length + lf.length + value.length;
        final ByteBuffer data = ByteBuffer.allocate(dataLength);
        data.put(key);
        data.put(lf);
        data.put(value);
        data.position(0);
        return formChunk(Util.byteBufferToBytes(data));
    }

    private byte[] formChunk(final byte[] data) {
        final byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
        final byte[] hexLength = Integer.toHexString(data.length)
                .getBytes(StandardCharsets.US_ASCII);
        final int chunkLength = data.length + 2*crlf.length + hexLength.length;
        final ByteBuffer chunk = ByteBuffer.allocate(chunkLength);
        chunk.put(hexLength);
        chunk.put(crlf);
        chunk.put(data);
        chunk.put(crlf);
        chunk.position(0);
        return Util.byteBufferToBytes(chunk);
    }
}
