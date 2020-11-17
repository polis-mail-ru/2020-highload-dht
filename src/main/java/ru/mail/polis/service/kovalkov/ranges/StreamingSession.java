package ru.mail.polis.service.kovalkov.ranges;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;

import static java.util.Objects.isNull;

public class StreamingSession extends HttpSession {
    private Iterator<Record> dataIterator;
    private static final byte[] CRLF = "\r\n".getBytes(Charset.defaultCharset());
    private static final byte[] SPLIT = "\n".getBytes(Charset.defaultCharset());
    private static final byte[] END_STREAM = "0\r\n\r\n".getBytes(Charset.defaultCharset());

    public StreamingSession(@NotNull final Socket socket, @NotNull final HttpServer server) {
        super(socket, server);
    }

    /**
     * Set iterator, mark response for chunked transferring, and init next method.
     *
     * @param dataIterator record iterator.
     * @throws IOException in case fault writeResponse.
     */
    public void setDataIterator(final Iterator<Record> dataIterator) throws IOException {
        this.dataIterator = dataIterator;
        final var response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        next();
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        next();
    }

    private void next() throws IOException {
        if (isNull(dataIterator)) throw new IllegalArgumentException("Iterator is null");
        while (dataIterator.hasNext() && isNull(queueHead)) {
            final byte[] chunk = preparedChunk(dataIterator.next());
            write(chunk,0, chunk.length);
        }
        if (dataIterator.hasNext()) return;
        write(END_STREAM, 0, END_STREAM.length);
        httpPipeliningHandler();
    }

    @NotNull
    private static byte[] preparedChunk(@NotNull final Record currentRecord) {
        final var key = BufferConverter.unfoldToBytes(currentRecord.getKey());
        final var value = BufferConverter.unfoldToBytes(currentRecord.getValue());
        final var dataSize = key.length + SPLIT.length + value.length;
        final var hexLength = Integer.toHexString(dataSize).getBytes(Charset.defaultCharset());
        final var chunk = new byte[hexLength.length + CRLF.length + dataSize + CRLF.length];
        final var chunkBuffer = ByteBuffer.wrap(chunk);
        chunkBuffer.put(hexLength).put(CRLF).put(key).put(SPLIT).put(value).put(CRLF);
        return chunk;
    }

    private void httpPipeliningHandler() throws IOException {
        Request handling = this.handling;
        if (isNull(handling)) {
            throw new IOException("Out of order response");
        }
        server.incRequestsProcessed();
        final String connection = handling.getHeader("Connection: ");
        final boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        if (!keepAlive) scheduleClose();
        handling = pipeline.pollFirst();
        this.handling = handling;
        if (this.handling != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }
}
