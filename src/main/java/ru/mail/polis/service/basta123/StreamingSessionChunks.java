package ru.mail.polis.service.basta123;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * class to enable set of methods for feeding an HTTP client by request-specified data  - that's to be
 * transferred entirely as a stream of blocks (chunks) within just an ongoing client-server session.
 */
public class StreamingSessionChunks extends HttpSession {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NO_CONTENT = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    Iterator<Record> iterator;


    StreamingSessionChunks(@NotNull final Socket socket, @NotNull final HttpServer server) {
        super(socket, server);
    }

    /**
     * launches stream process, which provides getting chunked
     */
    void init(final Iterator<Record> iter) throws IOException {
        this.iterator = iter;
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
        if (iterator != null) {
            while (iterator.hasNext() && queueHead == null) {
                final Record rawRecord = iterator.next();
                final byte[] chunk = getChunk(rawRecord);
                write(chunk, 0, chunk.length);
            }
            if (!iterator.hasNext()) {
                final byte[] noContentBytes = NO_CONTENT;
                write(noContentBytes, 0, NO_CONTENT.length);
                server.incRequestsProcessed();
                if ((handling = pipeline.pollFirst()) != null) {
                    if (handling == FIN) {
                        scheduleClose();
                    } else {
                        server.handleRequest(handling, this);
                    }
                }
            }
        }
        throw new IllegalArgumentException();
    }

    private byte[] getChunk(final Record record) {
        final byte[] key = Utils.getByteArrayFromByteBuffer(record.getKey());
        final byte[] value = Utils.getByteArrayFromByteBuffer(record.getValue());
        final byte[] data = new byte[key.length + LF.length + value.length];
        System.arraycopy(key, 0, data, 0, key.length);
        System.arraycopy(LF, 0, data, key.length, LF.length);
        System.arraycopy(value, 0, data, key.length + LF.length, value.length);

        final byte[] length = Integer.toHexString(data.length).getBytes(Charset.defaultCharset());
        final byte[] chunk = new byte[length.length + CRLF.length + data.length + CRLF.length];
        final ByteBuffer buffer = ByteBuffer.wrap(chunk);
        buffer.put(length);
        buffer.put(CRLF);
        buffer.put(data);
        buffer.put(CRLF);
        return chunk;
    }
}
