package ru.mail.polis.service.ivanovandrey;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;
import ru.mail.polis.dao.Timestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ServiceSession extends HttpSession {
    public static final byte[] CRLR = "\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] END = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private Iterator<Record> iterator;

    /**
     * Constructor.
     */
    public ServiceSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Set iterator to necessary position.
     * @param iter - iterator in position.
     */
    public void setIterator(final Iterator<Record> iter) throws IOException {
        this.iterator = iter;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        next();
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (iterator != null) {
            next();
        }
    }

    private byte[] toChunk(final Record elem) throws IOException {
        final byte[] dataKey = Util.fromByteBufferToByteArray(elem.getKey());
        final byte[] dataValue = Timestamp.getTimestampByData(elem.getValue()).getData();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(dataKey);
        outputStream.write('\n');
        outputStream.write(dataValue);
        final byte[] data = outputStream.toByteArray();
        final byte[] length = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[length.length + CRLR.length + data.length + CRLR.length];
        final ByteBuffer buffer = ByteBuffer.wrap(chunk);
        buffer.put(length);
        buffer.put(CRLR);
        buffer.put(data);
        buffer.put(CRLR);
        return chunk;
    }

    private void nextWrite() throws IOException {
        while (iterator.hasNext() && queueHead == null) {
            final var elem = iterator.next();
            final var chunk = toChunk(elem);
            write(chunk, 0, chunk.length);
        }
    }
    
    private void next() throws IOException {
        nextWrite();

        if (!iterator.hasNext()) {
            write(END, 0, END.length);

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
}
