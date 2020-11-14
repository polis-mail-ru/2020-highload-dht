package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.Socket;
import ru.mail.polis.Record;
import ru.mail.polis.service.Mapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static ru.mail.polis.service.stasyanoi.Constants.*;

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
        next();
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
            Record record = rocksIterator.next();
            byte[] data = getRecordData(record);
            write(data, 0, data.length);
        }

        if (rocksIterator.hasNext()) {
            return;
        }
        write(EOF,0 , EOF.length);

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

    private byte[] getRecordData(Record record) {
        byte[] key = Mapper.toBytes(record.getKey());
        byte[] value = Mapper.toBytes(record.getValue());
        byte[] valueWithoutTimestamp = new byte[value.length - TIMESTAMP_LENGTH];
        byte[] emptyBody = new byte[0];
        if (valueWithoutTimestamp.length == 1) {
          valueWithoutTimestamp = emptyBody;
        } else {
            System.arraycopy(value, 0, valueWithoutTimestamp, 0, value.length - TIMESTAMP_LENGTH);
        }
        byte[] data = new byte[key.length + EOL.length + valueWithoutTimestamp.length];
        ByteBuffer byteBufferData = ByteBuffer.wrap(data);
        byteBufferData.put(key);
        byteBufferData.put(EOL);
        byteBufferData.put(valueWithoutTimestamp);
        byte[] dataLength = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
        byte[] chunk = new byte[dataLength.length + CRLF.length + data.length + CRLF.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);
        byteBuffer.put(dataLength);
        byteBuffer.put(CRLF);
        byteBuffer.put(data);
        byteBuffer.put(CRLF);
        return chunk;
    }
}
