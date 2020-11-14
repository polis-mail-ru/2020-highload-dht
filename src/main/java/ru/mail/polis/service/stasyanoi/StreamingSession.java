package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
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

    public void setRocksIterator(Iterator<Record> rocksIterator) throws IOException {
        this.rocksIterator = rocksIterator;
        final Response response = Util.responseWithNoBody(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response,false);
    }

    @Override
    public synchronized void sendResponse(Response response) throws IOException {
        super.sendResponse(response);
    }

    protected void writeResponse(Response response, boolean includeBody) throws IOException {
        byte[] bytes = response.toBytes(includeBody);
        super.write(bytes, 0, bytes.length);

    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();

        if (rocksIterator != null) {
            next();
        }
    }

    private synchronized void next() throws IOException {
        while (rocksIterator.hasNext() && queueHead == null) {
            Record next = rocksIterator.next();
//            byte[] key = Mapper.toBytes(next.getKey());
//            byte[] value = Mapper.toBytes(next.getValue());
//            byte[] data = new byte[key.length + 1 + value.length];
//            System.arraycopy(key, 0, data, 0, key.length);
            //data[key.length] = "\n".getBytes(StandardCharsets.US_ASCII);
        }

        if (rocksIterator.hasNext()) {
            return;
        }

        String connection = handling.getHeader("Connection: ");
        boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        server.incRequestsProcessed();
        if (!keepAlive) scheduleClose();
        if ((this.handling = handling = pipeline.pollFirst()) != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }
}
