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

import static ru.mail.polis.service.stasyanoi.Constants.CONNECTION_HEADER_NAME;
import static ru.mail.polis.service.stasyanoi.Constants.CRLF;
import static ru.mail.polis.service.stasyanoi.Constants.EOF;
import static ru.mail.polis.service.stasyanoi.Constants.EOL;
import static ru.mail.polis.service.stasyanoi.Constants.TIMESTAMP_LENGTH;
import static ru.mail.polis.service.stasyanoi.Constants.TRANSFER_ENCODING_HEADER_NAME;

public class StreamingSession extends HttpSession {

    private Iterator<Record> rocksIterator;

    public StreamingSession(Socket socket, HttpServer server) {
        super(socket, server);
    }

    /**
     * Send a stream back to client from the given iterator.
     *
     * @param rocksIterator - iterator.
     * @throws IOException - if an io exception occurs whilst sending the response.
     */
    public void sendStreamResponse(Iterator<Record> rocksIterator) throws IOException {
        this.rocksIterator = rocksIterator;
        openStream();
        sendStream();
        closeStream();
    }

    private void openStream() throws IOException {
        final Response response = Util.responseWithNoBody(Response.OK);
        response.addHeader(TRANSFER_ENCODING_HEADER_NAME + "chunked");
        writeResponse(response,false);
    }

    private synchronized void sendStream() throws IOException {
        while (rocksIterator.hasNext() && queueHead == null) {
            final Record record = rocksIterator.next();
            final byte[] data = getRecordData(record);
            write(data, 0, data.length);
        }
    }

    private void closeStream() throws IOException {
        write(EOF,0, EOF.length);
        final String connection = handling.getHeader(CONNECTION_HEADER_NAME);
        final boolean keepAlive = handling.isHttp11() ? !"close".equalsIgnoreCase(connection)
                : "Keep-Alive".equalsIgnoreCase(connection);
        server.incRequestsProcessed();
        if (!keepAlive) scheduleClose();
        this.handling = pipeline.pollFirst();
        if (this.handling != null) {
            if (handling == FIN) {
                scheduleClose();
            } else {
                server.handleRequest(handling, this);
            }
        }
    }

    private byte[] getRecordData(final Record record) {
        final byte[] key = Mapper.toBytes(record.getKey());
        final byte[] value = Mapper.toBytes(record.getValue());
        byte[] valueWithoutTimestamp = new byte[value.length - TIMESTAMP_LENGTH];
        final byte[] emptyBody = new byte[0];
        if (valueWithoutTimestamp.length == 1) {
          valueWithoutTimestamp = emptyBody;
        } else {
            System.arraycopy(value, 0, valueWithoutTimestamp, 0, value.length - TIMESTAMP_LENGTH);
        }
        final byte[] data = new byte[key.length + EOL.length + valueWithoutTimestamp.length];
        final ByteBuffer byteBufferData = ByteBuffer.wrap(data);
        byteBufferData.put(key);
        byteBufferData.put(EOL);
        byteBufferData.put(valueWithoutTimestamp);
        final byte[] dataLength = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[dataLength.length + CRLF.length + data.length + CRLF.length];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);
        byteBuffer.put(dataLength);
        byteBuffer.put(CRLF);
        byteBuffer.put(data);
        byteBuffer.put(CRLF);
        return chunk;
    }
}
