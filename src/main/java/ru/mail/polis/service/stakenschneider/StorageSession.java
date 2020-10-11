package ru.mail.polis.service.stakenschneider;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

final class StorageSession extends HttpSession {
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte LF = '\n';
    private static final byte[] EMPTY_CHUNK = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    private Iterator<Record> records;

    StorageSession(@NotNull final Socket socket,
                   @NotNull final HttpServer server) {
        super(socket, server);
    }

    /**
     * Start streaming data from iterator.
     *
     * @param records - data
     * @throws IOException - network errors occurred
     */
    void stream(@NotNull final Iterator<Record> records) throws IOException {
        this.records = records;

        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);

        next();
    }

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        final byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();

        next();
    }

    private byte[] getByteArr(final int size) {
        return new byte[size];
    }

    private void next() throws IOException {
        while (records.hasNext() && queueHead == null) {
            final Record record = records.next();
            final byte[] key = toByteArray(record.getKey());
            final byte[] value = toByteArray(record.getValue());

            final int payloadLength = key.length + 1 + value.length;
            final String size = Integer.toHexString(payloadLength);

            final int chunkLength = size.length() + 2 + payloadLength + 2;

            final byte[] chunk = getByteArr(chunkLength);
            final ByteBuffer buffer = ByteBuffer.wrap(chunk);

            buffer.put(size.getBytes(StandardCharsets.UTF_8));
            buffer.put(CRLF);
            buffer.put(key);
            buffer.put(LF);
            buffer.put(value);
            buffer.put(CRLF);
            write(chunk, 0, chunkLength);
        }

        if (!records.hasNext()) {
            write(EMPTY_CHUNK, 0, EMPTY_CHUNK.length);

            server.incRequestsProcessed();

            if ((handling = pipeline.pollFirst()) != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    try {
                        server.handleRequest(handling, this);
                    } catch (IOException e) {
                        log.error("Cant process next request: " + handling, e);
                    }
                }
            }
        }
    }
}
