package ru.mail.polis.service.s3ponia;

import com.google.common.base.Charsets;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class StreamingSession extends HttpSession {
    private static final Logger logger = LoggerFactory.getLogger(StreamingSession.class);
    private static final String CHUNK_HEADER =
            "Transfer-Encoding: chunked";
    private static final byte[] CARRIAGE_RETURN_LINE_FEED =
            "\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] END_OF_FILE =
            "0\r\n\r\n".getBytes(Charsets.UTF_8);

    private Iterator<StreamingValue> valueIterator;

    public StreamingSession(@NotNull final Socket socket,
                            @NotNull final HttpServer server) {
        super(socket, server);
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        next();
    }

    /**
     * Starts streaming values from iterator.
     *
     * @param streamValues Streaming values
     * @throws IOException rethrow from next
     */
    public void stream(@NotNull final Iterator<StreamingValue> streamValues) throws IOException {
        this.valueIterator = streamValues;
        final Response response = new Response(Response.OK);
        response.addHeader(CHUNK_HEADER);
        writeResponse(response, false);
        next();
    }

    private void next() throws IOException {
        while (valueIterator.hasNext() && queueHead == null) {
            final var value = valueIterator.next();
            final var sendSize = value.valueSize();
            final var stringSize = Integer.toHexString(sendSize);
            final var chunkSize = stringSize.length() + CARRIAGE_RETURN_LINE_FEED.length
                    + sendSize + CARRIAGE_RETURN_LINE_FEED.length;
            final byte[] chunk = new byte[chunkSize];
            final var wrapBuffer = ByteBuffer.wrap(chunk);
            wrapBuffer
                    .put(stringSize.getBytes(Charsets.UTF_8))
                    .put(CARRIAGE_RETURN_LINE_FEED);
            value
                    .value(wrapBuffer);
            wrapBuffer
                    .put(CARRIAGE_RETURN_LINE_FEED);

            write(chunk, 0, chunkSize);
        }

        if (!valueIterator.hasNext()) {
            write(END_OF_FILE, 0, END_OF_FILE.length);
            server.incRequestsProcessed();

            if ((handling = pipeline.pollFirst()) != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    try {
                        server.handleRequest(handling, this);
                    } catch (IOException exception) {
                        logger.error("Error in handling next request", exception);
                    }
                }
            }
        }
    }

}
