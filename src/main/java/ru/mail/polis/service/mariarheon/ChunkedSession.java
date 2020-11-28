package ru.mail.polis.service.mariarheon;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;

import java.io.IOException;
import java.util.Iterator;

public class ChunkedSession extends HttpSession {
    private Iterator<ru.mail.polis.Record> iterator;

    /**
     * Create HttpSession for writing chunked response.
     *
     * @param socket - socket
     * @param server - http server
     */
    public ChunkedSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Write range of records to response.
     *
     * @param iterator - iterator of records.
     */
    public void writeRangeResponse(final Iterator<ru.mail.polis.Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        next();
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (iterator != null) {
            next();
        }
    }

    private void next() throws IOException {
        writeNext();
        writePost();
    }

    private void writeNext() throws IOException {
        while (iterator.hasNext() && queueHead == null) {
            final var elem = iterator.next();
            ChunkedEncoder.write(this, elem);
        }
    }

    private void writePost() throws IOException {
        if (!iterator.hasNext()) {
            ChunkedEncoder.writeLastChunk(this);

            Request handling = this.handling;
            if (handling == null) {
                throw new IOException("Out of order response");
            }
            server.incRequestsProcessed();
            final String connection = handling.getHeader("Connection: ");
            final boolean keepAlive = handling.isHttp11()
                    ? !"close".equalsIgnoreCase(connection)
                    : "Keep-Alive".equalsIgnoreCase(connection);
            if (!keepAlive) scheduleClose();
            this.handling = handling = pipeline.pollFirst();
            if (handling != null) {
                if (handling == FIN) {
                    scheduleClose();
                } else {
                    server.handleRequest(handling, this);
                }
            }
        }
    }
}
