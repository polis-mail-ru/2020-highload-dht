package ru.mail.polis.service.kate.moreva;

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

/**
 * Simple Http Session for chunk responses.
 */
public class StreamingSession extends HttpSession {
    private static final String CONNECTION_HEADER = "Connection:";
    private static final String TRANSFER_HEADER = "Transfer-Encoding: chunked";
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EOC = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private Iterator<Record> recordIterator;

    public StreamingSession(@NotNull final Socket socket,
                            @NotNull final HttpServer server) {
        super(socket, server);
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();
        if (recordIterator != null) {
            pushNext();
        }
    }

    public void setRecordIterator(final Iterator<Record> recordIterator) throws IOException {
        if (recordIterator == null) {
            throw new IOException("Iterator is null");
        }
        this.recordIterator = recordIterator;
        final Response response = new Response(Response.OK);
        response.addHeader(TRANSFER_HEADER);
        writeResponse(response, false);
        pushNext();
    }

    private synchronized void pushNext() throws IOException {
        while (recordIterator.hasNext() && queueHead == null) {
            byte[] data = makeChunk(recordIterator.next());
            write(data, 0, data.length);
        }
        if (recordIterator.hasNext()) {
            return;
        }
        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order");
        }
        write(EOC, 0, EOC.length);
        server.incRequestsProcessed();
        final String connection = handling.getHeader(CONNECTION_HEADER);
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

    private byte[] makeChunk(final Record record) {
        final ByteBuffer key = record.getKey();
        final ByteBuffer value = record.getValue();
        final int dataLength = key.remaining() + LF.length + value.remaining();
        final byte[] records = toByteArray(ByteBuffer.allocate(dataLength).put(key).put(LF).put(value).position(0));
        final byte[] length = Integer.toHexString(records.length).getBytes(StandardCharsets.UTF_8);
        return toByteArray(ByteBuffer.allocate(records.length + 2 * CRLF.length + length.length)
                .put(length).put(CRLF).put(records).put(CRLF).position(0));
    }

    public static byte[] toByteArray(final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }
}
