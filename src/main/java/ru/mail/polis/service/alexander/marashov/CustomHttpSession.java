package ru.mail.polis.service.alexander.marashov;

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

public class CustomHttpSession extends HttpSession {

    private static final byte[] EOC = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    private Iterator<Record> recordIterator;

    public CustomHttpSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();

        if (recordIterator != null) {
            pushWhileFits();
        }
    }

    public void sendRecords(final Iterator<Record> iterator) throws IOException {
        this.recordIterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);

        pushWhileFits();
    }

    private void pushWhileFits() throws IOException {
        while (recordIterator.hasNext() && queueHead == null) {
            final Record record = recordIterator.next();
            final byte[] data = createChunk(record);
            write(data, 0, data.length);
        }

        if (recordIterator.hasNext()) {
            return;
        }

        write(EOC, 0, EOC.length);

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
        if (this.handling != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }

    }

    private static byte[] createChunk(final Record record) {
        final ByteBuffer key = record.getKey();
        final ByteBuffer value = record.getValue();

        final int keyLength = key.remaining();
        final int valueLength = value.remaining();

        final int dataLength = keyLength + LF.length + valueLength;
        final byte[] length = Integer.toHexString(dataLength).getBytes(StandardCharsets.UTF_8);
        final ByteBuffer serializedData = ByteBuffer.allocate(length.length + CRLF.length + dataLength + CRLF.length);

        serializedData.put(length);
        serializedData.put(CRLF);
        serializedData.put(key);
        serializedData.put(LF);
        serializedData.put(value);
        serializedData.put(CRLF);
        serializedData.position(0);

        return serializedData.array();
    }
}
