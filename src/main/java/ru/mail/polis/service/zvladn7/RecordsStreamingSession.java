package ru.mail.polis.service.zvladn7;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import static ru.mail.polis.service.zvladn7.util.Bytes.toBytes;

public class RecordsStreamingSession extends HttpSession {

    private static final byte[] CRLF = toBytes("\r\n");
    private static final byte[] LF = toBytes("\n");
    private static final byte[] EOF = toBytes("0\r\n\r\n");
    private static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding: chunked";

    private Iterator<Record> recordIterator;

    public RecordsStreamingSession(@NotNull final Socket socket,
                                   @NotNull final HttpServer server) {
        super(socket, server);
    }

    public void setIterator(@NotNull final Iterator<Record> iterator) throws IOException {
        this.recordIterator = iterator;

        final Response response = new Response(Response.OK);
        response.addHeader(TRANSFER_ENCODING_HEADER);
        writeResponse(response, false);
        tryToProvideNext();
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (recordIterator != null) tryToProvideNext();
    }

    private void tryToProvideNext() throws IOException {
        while (recordIterator.hasNext() && queueHead == null) {
            final Record record = recordIterator.next();
            final byte[] chunk = recordToChunk(record);
            write(chunk, 0, chunk.length);
        }

        if (recordIterator.hasNext()) {
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
        if (!keepAlive) scheduleClose();

        if ((this.handling = handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }

    @NotNull
    private static byte[] recordToChunk(@NotNull final Record record) {
        final byte[] keyBytes = toBytes(record.getKey());
        final byte[] valueBytes = toBytes(record.getValue());
        final int dataSize = keyBytes.length + LF.length + valueBytes.length;
        final byte[] length = toBytes(Integer.toHexString(dataSize));

        final int chunkSize = getChunkSize(dataSize, length.length);
        final byte[] chunk = new byte[chunkSize];
        final ByteBuffer buffer = ByteBuffer.wrap(chunk);
        buffer.put(length)
                .put(CRLF)
                .put(keyBytes)
                .put(LF)
                .put(valueBytes)
                .put(CRLF);

        return chunk;
    }

    private static int getChunkSize(final int dataSize, final int lengthSize) {
        return dataSize + 2 * CRLF.length + lengthSize;
    }
}
