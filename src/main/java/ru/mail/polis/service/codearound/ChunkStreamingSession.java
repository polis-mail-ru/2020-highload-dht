package ru.mail.polis.service.codearound;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 *  class to enable set of methods for feeding an HTTP client by request-specified data  - that's to be
 *  transferred entirely as a stream of blocks (chunks) within just an ongoing client-server session.
 */
public class ChunkStreamingSession extends HttpSession {

    private static final byte[] bytesOfCRLF = "\r\n".getBytes(Charset.defaultCharset());
    private static final byte[] bytesOfLF = "\n".getBytes(Charset.defaultCharset());
    private static final byte[] nullContentBytes = "0\r\n\r\n".getBytes(Charset.defaultCharset());
    private static final String ENCODING_HEADER = "Transfer-Encoding: chunked";
    private Iterator<Record> it;

    /**
     * class const.
     *
     * @param socket - socket to receive chunks on client host
     * @param server - HTTP server requested for data
     */
    ChunkStreamingSession(@NotNull final Socket socket, @NotNull final HttpServer server) {
        super(socket, server);
    }

    /**
     * launches stream process of writes while ongoing HTTP session.
     */
    void initStreaming(final Iterator<Record> it) throws IOException {
        this.it = it;
        final Response response = new Response(Response.OK);
        response.addHeader(ENCODING_HEADER);
        writeResponse(response, false);
        nextWrite();
    }

    /**
     * starts writing chunks from buffer as subsequent operations.
     */
    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        nextWrite();
    }

    /**
     * wraps method call to execute writing each separate chunk from buffer.
     */
    private void nextWrite() throws IOException {
        if (it == null) {
            throw new IllegalArgumentException();
        }

        while (it.hasNext() && queueHead == null) {
            final Record rawRecord = it.next();
            final byte[] chunk = getBytesFromRecord(rawRecord);
            write(chunk, 0, chunk.length);
        }
        if (!it.hasNext()) {
            commitTailHandling();
        }
    }

    /**
     * retrieves a chunk which is made up primarily by record ID (key) and some value coupled, also by adding
     * CRLF and LF byte sequences to parse the input orderly.
     *
     * @param record - specific record a chunk is composed from
     * @return chunk of data wrapped into byte array
     */
    private byte[] getBytesFromRecord(@NotNull final Record record) {

        final byte[] key = DAOByteOnlyConverter.readByteArray(record.getKey());
        final byte[] value = DAOByteOnlyConverter.readByteArray(record.getValue());
        final int recordSize = key.length + bytesOfLF.length + value.length;
        final String base16RecordSize = Integer.toHexString(recordSize);
        final int chunkSize = base16RecordSize.length() + recordSize + 2 * bytesOfCRLF.length;
        final byte[] recordBytes = base16RecordSize.getBytes(Charset.defaultCharset());
        final byte[] chunkArray = new byte[chunkSize];
        final ByteBuffer chunkBuf = ByteBuffer.wrap(chunkArray);
        chunkBuf.put(recordBytes)
                .put(bytesOfCRLF)
                .put(key)
                .put(bytesOfLF)
                .put(value)
                .put(bytesOfCRLF);

        return chunkArray;
    }

    /**
     * handles end of chunk consumed.
     */
    private void commitTailHandling() throws IOException {
        write(nullContentBytes, 0, nullContentBytes.length);
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
