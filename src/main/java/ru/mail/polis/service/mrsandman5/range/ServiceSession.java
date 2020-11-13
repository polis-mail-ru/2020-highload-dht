package ru.mail.polis.service.mrsandman5.range;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Iterator;

@ThreadSafe
public class ServiceSession extends HttpSession {
    private ChunksProvider chunks;

    public ServiceSession(@NotNull final Socket socket,
                          @NotNull final HttpServer server) {
        super(socket, server);
    }

    public void stream(@NotNull final Iterator<Record> iterator) throws IOException {
        this.chunks = new ChunksProvider(iterator);
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        next();
    }

    private void next() throws IOException {
        while (chunks.hasNext() && queueHead == null) {
            final byte[] data = chunks.next();
            write(data, 0, data.length);
        }
        if (!chunks.hasNext()) {
            final byte[] end = chunks.end();
            write(end, 0, end.length);
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

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();
        if (chunks != null) {
            next();
        }
    }

}
