package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.Closeable;
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
    private static final byte[] EOL = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EOF = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private Iterator<Record> recordIterator;

    public StreamingSession(@NotNull final Socket socket,
                            @NotNull final HttpServer server) {
        super(socket, server);
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();
        if (recordIterator != null) {
            next();
        }
    }

    public void setRecordIterator(final Iterator<Record> recordIterator) throws IOException {
        if (recordIterator == null) {
            throw new IOException("Iterator is null");
        }
        this.recordIterator = recordIterator;
        final Response response = new Response(Response.OK);
        response.addHeader(keepAlive() ? "Connection: Keep-Alive" : "Connection: close");
        response.addHeader(TRANSFER_HEADER);
        writeResponse(response, false);
        pushNext();
    }


    private boolean keepAlive() {
        final var connection = handling.getHeader("Connection: ");
        return handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
    }

    private synchronized void pushNext() throws IOException {
        while (recordIterator.hasNext() && queueHead == null) {
            Record record = recordIterator.next();
            MySimpleHttpServer.log.info("Record" + record.getValue());
            final byte[] data = makeChunk(record);
            write(data, 0, data.length);
        }
        if (recordIterator.hasNext()) {
            return;
        }
        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order");
        }
        write(EOF, 0, EOF.length);
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
    public synchronized void stream(final Iterator<Record> recordIterator) throws IOException {
        this.recordIterator = recordIterator;
        if (handling == null) {
            throw new IOException("Out of order response");
        }
        final var response = new Response(Response.OK);
        response.addHeader(keepAlive() ? "Connection: Keep-Alive" : "Connection: close");
        response.addHeader("Transfer-Encoding: chunked");

        writeResponse(response, false);

        next();
    }

    private void handleStreamEnding() throws IOException {
        write(EOF, 0, EOF.length);
        server.incRequestsProcessed();

        if (!keepAlive()) {
            scheduleClose();
        }
        if ((handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }
    private void writeRecord(final Record record) throws IOException {
        final var key = toArray(record.getKey());
        final var value = toArray(record.getValue());

        final var payloadLength = key.length + EOL.length + value.length;
        final var payloadLengthHex = Integer.toHexString(payloadLength);
        final var chunkLength =
                payloadLengthHex.length() + CRLF.length + payloadLength + CRLF.length;

        final var chunk = new byte[chunkLength];
        final var chunkBuffer = ByteBuffer.wrap(chunk);
        chunkBuffer.put(payloadLengthHex.getBytes(StandardCharsets.UTF_8));
        chunkBuffer.put(CRLF);
        chunkBuffer.put(key);
        chunkBuffer.put(EOL);
        chunkBuffer.put(value);
        chunkBuffer.put(CRLF);

        write(chunk, 0, chunk.length);
    }

    private synchronized void next() throws IOException {
        if (recordIterator == null) {
            throw new IllegalStateException("Iterator is missing");
        }
        while (recordIterator.hasNext() && queueHead == null) {
            final var record = recordIterator.next();
            writeRecord(record);
        }
        if (!recordIterator.hasNext()) {
            handleStreamEnding();
            if (recordIterator instanceof Closeable) {
                try {
                    ((Closeable) recordIterator).close();
                } catch (IOException exception) {
                    log.error("Exception while close iterator", exception);
                }
            }
            recordIterator = null;
        }
    }
    public byte[] toArray(@NotNull final ByteBuffer buffer) {
        final var bufferCopy = buffer.duplicate();
        final var array = new byte[bufferCopy.remaining()];
        bufferCopy.get(array);
        return array;
    }

    private byte[] makeChunk(final Record record) {
        final ByteBuffer key = record.getKey();
        final ByteBuffer value = record.getValue();
        final int dataLength = key.remaining() + EOL.length + value.remaining();
        final byte[] length = Integer.toHexString(dataLength).getBytes(StandardCharsets.UTF_8);
        final byte[] chunk = new byte[dataLength + 2 * CRLF.length + length.length];
        return ByteBuffer.wrap(chunk)
                .put(length)
                .put(CRLF)
                .put(key)
                .put(EOL)
                .put(value)
                .put(CRLF)
                .array();
    }
}
