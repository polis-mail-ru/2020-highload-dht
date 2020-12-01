package ru.mail.polis.service.manikhin;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;

import java.io.IOException;
import java.util.Iterator;

public class StreamSession extends HttpSession {
    private Iterator<Record> iterator;

    /**
     * Config StreamSession.
     */
    public StreamSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Set iterator, form 200-OK response and start sending chunks.
     */
    public void setIterator(final Iterator<Record> iterator) throws IOException {
        this.iterator = iterator;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response,false);
        stream();
    }

    @Override
    public void processWrite() throws Exception {
        super.processWrite();

        if (iterator != null) {
            stream();
        }
    }

    private void stream() throws IOException {
        byte[] data;
        while (iterator.hasNext() && queueHead == null) {
            final Record record = iterator.next();

            data = StreamUtils.formFilledChunk(record.getKey(), record.getValue());
            write(data,0,data.length);
        }

        if (!iterator.hasNext()) {
            data = StreamUtils.formEndChunk();
            write(data,0,data.length);

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
