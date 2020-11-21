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

import static ru.mail.polis.service.stasyanoi.Constants.CRLF;
import static ru.mail.polis.service.stasyanoi.Constants.EOF;
import static ru.mail.polis.service.stasyanoi.Constants.EOL;

public class StreamingSession extends HttpSession {

    private Iterator<Record> rocksIterator;

    public StreamingSession(final Socket socket, final HttpServer server) {
        super(socket, server);
    }

    @Override
    protected void processWrite() throws Exception {
        super.processWrite();

        if (rocksIterator != null) {
            sendStream();
            if (!rocksIterator.hasNext()) {
                closeStream();
            }
        }
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
        if (!rocksIterator.hasNext()) {
            closeStream();
        }
    }

    private void openStream() throws IOException {
        final Response response = Util.responseWithNoBody(Response.OK);
        response.addHeader(Constants.TRANSFER_ENCODING_HEADER_NAME + "chunked");
        writeResponse(response, false);
    }

    private synchronized void sendStream() throws IOException {
        while (rocksIterator.hasNext() && queueHead == null) {
            final Record record = rocksIterator.next();
            final byte[] data = getRecordData(record);
            write(data, 0, data.length);
        }
    }

    private void closeStream() throws IOException {
        write(EOF, 0, EOF.length);
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
        final int valueLengthWithoutTimestamp = value.length == Constants.EMPTY_BODY_SIZE
                ? 0 : value.length - Constants.TIMESTAMP_LENGTH;
        final byte[] dataLength = Integer.toHexString(key.length + EOL.length + valueLengthWithoutTimestamp)
                .getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[dataLength.length + CRLF.length + key.length + EOL.length
                + valueLengthWithoutTimestamp + CRLF.length];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);
        byteBuffer.put(dataLength);
        byteBuffer.put(CRLF);
        byteBuffer.put(key);
        byteBuffer.put(EOL);
        byteBuffer.put(value, 0, valueLengthWithoutTimestamp);
        byteBuffer.put(CRLF);
        return chunk;
    }
}
