package ru.mail.polis.service.nik27090;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;

import java.io.IOException;

public class RecordStreamingSession extends HttpSession {
    private static final String CHUNK_HEADER = "Transfer-Encoding: chunked";

    private ChunkIterator chunks;
    private final HttpHelper httpHelper;

    public RecordStreamingSession(
            final Socket socket,
            final HttpServer server,
            final HttpHelper httpHelper) {
        super(socket, server);
        this.httpHelper = httpHelper;
    }

    /**
     * Set field chunks.
     *
     * @param chunkIterator - chunk Iterator
     */
    public void setIterator(HttpSession session, final ChunkIterator chunkIterator) {
        this.chunks = chunkIterator;

        final Response response = new Response(Response.OK, Response.EMPTY);
        response.addHeader(CHUNK_HEADER);

        try {
            writeResponse(response, false);
            next();
        } catch (IOException e) {
            log.error("Socket internal error", e);
            httpHelper.sendResponse(session, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (chunks != null) {
            next();
        }
    }

    private void next() throws IOException {
        while (chunks.hasNext() && queueHead == null) {
            final byte[] chunk = chunks.next();
            write(chunk, 0, chunk.length);
        }

        if (!chunks.hasNext()) {
            closeSession();
        }
    }

    private void closeSession() throws IOException {
        final Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }

        final byte[] end = chunks.end();
        write(end, 0, end.length);

        server.incRequestsProcessed();

        final String connection = handling.getHeader("Connection: ");
        final boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        if (!keepAlive) scheduleClose();
        if ((this.handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }
}
