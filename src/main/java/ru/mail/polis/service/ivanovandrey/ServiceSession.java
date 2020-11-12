package ru.mail.polis.service.ivanovandrey;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.rocksdb.RocksIterator;
import ru.mail.polis.Record;
import ru.mail.polis.dao.RecordIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServiceSession extends HttpSession {
    public static final byte[] CRLR = "\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] END = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private ChunkedIterator iterator;

    public ServiceSession(Socket socket, HttpServer server) {
        super(socket, server);
    }
    
    public void setIterator(final ChunkedIterator iter) throws IOException {
        this.iterator = iter;
        final Response response = new Response(Response.OK);
        response.addHeader("Transfer-Encoding: chunked");
        writeResponse(response, false);
        next();
    }

    @Override
    protected void processWrite() throws Exception{
        super.processWrite();
        if (iterator != null)
        next();
    }

    private void next() throws IOException {
        while (iterator.hasNext() && queueHead == null){
            final var elem = iterator.next();
            final byte[] dataKey = Util.fromByteBufferToByteArray(elem.getKey());
            final byte[] dataValue = Util.fromByteBufferToByteArray(elem.getValue());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write( dataKey );
            outputStream.write('\n');
            outputStream.write( dataValue );
            final byte[] data = outputStream.toByteArray( );
            final byte[] length = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
            final byte[] chunk = new byte[length.length + CRLR.length + data.length + CRLR.length];
            final ByteBuffer buffer = ByteBuffer.wrap(chunk);
            buffer.put(length);
            buffer.put(CRLR);
            buffer.put(data);
            buffer.put(CRLR);
            write(chunk, 0, chunk.length);
        }

        if(!iterator.hasNext()){
            return;
        }

        write(END, 0 , END.length);

        Request handling = this.handling;
        if (handling == null) {
            throw new IOException("Out of order response");
        }

        server.incRequestsProcessed();

        String connection = handling.getHeader("Connection: ");
        boolean keepAlive = handling.isHttp11()
                ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        if (!keepAlive) scheduleClose();
        if ((this.handling = handling = pipeline.pollFirst()) != null){
            if (handling == FIN){
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }
}
