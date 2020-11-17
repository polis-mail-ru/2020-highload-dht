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

    public RecordStreamingSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Set field chunks.
     *
     * @param chunkIterator - chunk Iterator
     * @throws IOException - throws IOException
     */
    public void setIterator(final ChunkIterator chunkIterator) throws IOException {
        this.chunks = chunkIterator;

        final Response response = new Response(Response.OK, Response.EMPTY);
        response.addHeader(CHUNK_HEADER);
        writeResponse(response, false);
        next();
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
}
