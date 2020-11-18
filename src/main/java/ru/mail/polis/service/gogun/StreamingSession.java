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

    public StreamingSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Method provides getting chunked response.
     *
     * @param iterator - iterator on data
     * @throws IOException - error
     */
    public void setIterator(final Iterator<Record> iterator) throws IOException {
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

        final Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }

        server.incRequestsProcessed();
        final String connection = handling.getHeader("Connection: ");
        final boolean keepAlive = handling.isHttp11()
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
        final int dataLength = key.length + LF.length + value.length;
        final byte[] length = Integer.toHexString(dataLength).getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[length.length + CRLF.length + dataLength + CRLF.length];
        final ByteBuffer buffer = ByteBuffer.wrap(chunk);
        buffer.put(length);
        buffer.put(CRLF);
        buffer.put(key);
        buffer.put(LF);
        buffer.put(value);
        buffer.put(CRLF);

        return chunk;
    }
}
