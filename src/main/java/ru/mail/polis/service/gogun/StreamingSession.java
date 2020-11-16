package ru.mail.polis.service.gogun;

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

public class StreamingSession extends HttpSession {
    public static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    public static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EOF = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    private Iterator<Record> iterator;

    public StreamingSession(Socket socket, HttpServer server) {
        super(socket, server);
    }

    public void setIterator(Iterator<Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        pushWhileFits();
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (iterator != null) {
            pushWhileFits();
        }
    }

    private void pushWhileFits() throws IOException {

        while (iterator.hasNext() && queueHead == null) {
            final Record element = iterator.next();
            final byte[] chunk = getChunk(element);
            write(chunk, 0, chunk.length);
        }

        if (iterator.hasNext()) {
            return;
        }

        write(EOF, 0, EOF.length);

        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }

        server.incRequestsProcessed();
        String connection = handling.getHeader("Connection: ");
        boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        if (!keepAlive) {
            scheduleClose();
        }
        if ((this.handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }

    private byte[] getChunk(final Record record) {
        final byte[] key = ServiceUtils.getArray(record.getKey());
        final byte[] value = ServiceUtils.getArray(record.getValue());
        final byte[] data = new byte[key.length + LF.length + value.length];
        System.arraycopy(key, 0, data, 0, key.length);
        System.arraycopy(LF, 0, data, key.length, LF.length);
        System.arraycopy(value, 0, data, key.length + LF.length, value.length);

        final byte[] length = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[length.length + CRLF.length + data.length + CRLF.length];
        final ByteBuffer buffer = ByteBuffer.wrap(chunk);
        buffer.put(length);
        buffer.put(CRLF);
        buffer.put(data);
        buffer.put(CRLF);

        return chunk;
    }
}
