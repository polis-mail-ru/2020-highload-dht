package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;

import java.io.IOException;
import java.util.Iterator;

public class StreamingSession extends HttpSession {

    private Iterator<Record> rocksIterator;

    public StreamingSession(Socket socket, HttpServer server) {
        super(socket, server);
    }

    public void setRocksIterator(Iterator<Record> rocksIterator) {
        this.rocksIterator = rocksIterator;
        this.rocksIterator.next();
    }

    @Override
    public synchronized void sendResponse(Response response) throws IOException {
        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }

        server.incRequestsProcessed();

        String connection = handling.getHeader("Connection: ");
        boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        response.addHeader(keepAlive ? "Connection: Keep-Alive" : "Connection: close");

        writeResponse(response, handling.getMethod() != Request.METHOD_HEAD);
        if (!keepAlive) scheduleClose();

        if ((this.handling = handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }

    protected void writeResponse(Response response, boolean includeBody) throws IOException {
        byte[] bytes = response.toBytes(includeBody);
        super.write(bytes, 0, bytes.length);
    }
}
