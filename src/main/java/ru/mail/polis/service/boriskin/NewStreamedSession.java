package ru.mail.polis.service.boriskin;

import com.google.common.base.Charsets;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

final class NewStreamedSession extends HttpSession {
    private static final Logger logger = LoggerFactory.getLogger(NewStreamedSession.class);

    private static final String CHUNK_HEADER =
            "Transfer-Encoding: chunked";
    private static final byte[] END_OF_LINE =
            "\n".getBytes(Charsets.UTF_8);
    private static final byte[] CARRIAGE_RETURN_LINE_FEED =
            "\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] END_OF_FILE =
            "0\r\n\r\n".getBytes(Charsets.UTF_8);

    private Iterator<Record> recordIterator;

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        next();
    }

    NewStreamedSession(
            @NotNull final Socket socket,
            @NotNull final HttpServer httpServer) {
        super(socket, httpServer);
    }

    void stream(
            @NotNull final Iterator<Record> recordIterator) throws IOException {
        this.recordIterator = recordIterator;
        final Response response = new Response(Response.OK);
        response.addHeader(CHUNK_HEADER);
        writeResponse(response, false);
        next();
    }

    private void next() throws IOException {
        while (recordIterator.hasNext() && queueHead == null) {
            final Record record = recordIterator.next();
            final byte[] key = toByteArray(record.getKey());
            final byte[] val = toByteArray(record.getValue());
            final int payloadLength = key.length + END_OF_LINE.length + val.length;
            final String length = Integer.toHexString(payloadLength);
            final int chunkLength =
                    length.length()
                            + CARRIAGE_RETURN_LINE_FEED.length
                            + payloadLength
                            + CARRIAGE_RETURN_LINE_FEED.length;
            final byte[] chunk = new byte[chunkLength];
            final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);

            byteBuffer.put(length.getBytes(Charsets.UTF_8));
            byteBuffer.put(CARRIAGE_RETURN_LINE_FEED);
            byteBuffer.put(key);
            byteBuffer.put(END_OF_LINE);
            byteBuffer.put(val);
            byteBuffer.put(CARRIAGE_RETURN_LINE_FEED);

            write(chunk, 0, chunk.length);
        }
        if (!recordIterator.hasNext()) {
            write(END_OF_FILE, 0, END_OF_FILE.length);
            server.incRequestsProcessed();
            if ((handling = pipeline.pollFirst()) != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    try {
                        server.handleRequest(handling, this);
                    } catch (IOException ioException) {
                        logger.error("Следующая сессия: " + handling, ioException);
                    }
                }
            }
        }
    }

    @NotNull
    private byte[] toByteArray(
            @NotNull final ByteBuffer byteBuffer) {
        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }
}
