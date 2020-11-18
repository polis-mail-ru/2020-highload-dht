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
import java.util.Arrays;
import java.util.Iterator;

import static ru.mail.polis.service.stasyanoi.Constants.CRLF;
import static ru.mail.polis.service.stasyanoi.Constants.EOF;
import static ru.mail.polis.service.stasyanoi.Constants.EOL;

public class StreamingSession extends HttpSession {

    private Iterator<Record> rocksIterator;

    public StreamingSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    /**
     * Send a stream back to client from the given iterator.
     *
     * @param rocksIterator - iterator.
     * @throws IOException - if an io exception occurs whilst sending the response.
     */
    public void sendStreamResponse(final Iterator<Record> rocksIterator) throws IOException {
        this.rocksIterator = rocksIterator;
        openStream();
        sendStream();
        closeStream();
    }

    private void openStream() throws IOException {
        final Response response = Util.responseWithNoBody(Response.OK);
        response.addHeader(Constants.TRANSFER_ENCODING_HEADER_NAME + "chunked");
        writeResponse(response,false);
    }

    private synchronized void sendStream() throws IOException {
        boolean queueNotNull;
        do {
            while (rocksIterator.hasNext() && queueHead == null) {
                final Record record = rocksIterator.next();
                final byte[] data = getRecordData(record);
                write(data, 0, data.length);
            }
            queueNotNull = queueHead != null;
        } while (queueNotNull);
    }

    private void closeStream() throws IOException {
        write(EOF,0, EOF.length);
        final String connection = handling.getHeader(Constants.CONNECTION_HEADER_NAME);
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
        final byte[] valueWithoutTimestamp;
        final byte[] dataLength;
        final byte[] chunk;
        if (value.length == Constants.EMPTY_BODY_SIZE) {
            valueWithoutTimestamp = Constants.EMPTY_BODY;
        } else {
            final int pureBodyLength = value.length - Constants.TIMESTAMP_LENGTH;
            valueWithoutTimestamp = Arrays.copyOfRange(value, 0, pureBodyLength);
        }
        dataLength = Integer.toHexString(key.length + EOL.length + valueWithoutTimestamp.length)
                .getBytes(StandardCharsets.US_ASCII);
        chunk = new byte[dataLength.length + CRLF.length + key.length + EOL.length
                + valueWithoutTimestamp.length + CRLF.length];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);
        byteBuffer.put(dataLength);
        byteBuffer.put(CRLF);
        byteBuffer.put(key);
        byteBuffer.put(EOL);
        byteBuffer.put(valueWithoutTimestamp);
        byteBuffer.put(CRLF);
        return chunk;
    }
}
