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

    private static final String CRLF = "\r\n";
    private static final String LF = "\n";
    private static final String NO_CONTENT = "0\r\n\r\n";
    Iterator<Record> it;

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
        response.addHeader("Transfer-Encoding: chunked");
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
            final byte[] id = DAOByteOnlyConverter.readByteArray(rawRecord.getKey());
            final byte[] value = DAOByteOnlyConverter.readByteArray(rawRecord.getValue());
            final int recordString = id.length + LF.length() + value.length;
            final String base16RecordString = Integer.toHexString(recordString);
            final int chunkSize = base16RecordString.length() + CRLF.length() + recordString + CRLF.length();
            execChunkWrite(id, value, chunkSize, base16RecordString);
        }
        if (!it.hasNext()) {
            commitTailHandling();
        }
    }

    /**
     * executes immediate writing chunks from buffer in successive way.
     *
     * @param id - record match ID to search throughout the storage
     * @param value - record-wrapped value
     * @param recordString - String-formatted combination of record ID and value content
     * @param base16RecordString - record ID and value content joined to hexadecimal numeric presentation
     */
    private void execChunkWrite(final byte[] id,
                                final byte[] value,
                                final int recordString,
                                final String base16RecordString) throws IOException {
        final byte[] recordBytes = base16RecordString.getBytes(Charset.defaultCharset());
        final byte[] CRLFBytes = CRLF.getBytes(Charset.defaultCharset());
        final byte[] LFBytes = LF.getBytes(Charset.defaultCharset());
        final byte[] chunkArray = new byte[recordString];
        final ByteBuffer chunkBuf = ByteBuffer.wrap(chunkArray);
        chunkBuf.put(recordBytes);
        chunkBuf.put(CRLFBytes);
        chunkBuf.put(id);
        chunkBuf.put(LFBytes);
        chunkBuf.put(value);
        chunkBuf.put(CRLFBytes);
        write(chunkArray, 0, chunkArray.length);
    }

    /**
     * handles end of chunk consumed.
     */
    private void commitTailHandling() throws IOException {
        final byte[] noContentBytes = NO_CONTENT.getBytes(Charset.defaultCharset());
        ;
        write(noContentBytes, 0, NO_CONTENT.length());
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
