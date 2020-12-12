package ru.mail.polis.service.mariarheon;

import one.nio.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Encoder for writing chunks to the session in Transfer-Chunked-Encoding format.
 */
public class ChunkedEncoder {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);
    final HttpSession session;

    /**
     * Create encoder for writing chunks to the session.
     *
     * @param session - session.
     * @throws IOException - raised when writing to session failed.
     */
    public ChunkedEncoder(final HttpSession session) throws IOException {
        this.session = session;
        final var head = ("HTTP/1.1 200 OK\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Content-Type: text/plain\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n")
                .getBytes(StandardCharsets.UTF_8);
        session.write(head, 0, head.length);
    }

    /**
     * Write new chunk with the record information to session.
     *
     * @param record - key-value pair, which should be written as chunk to session.
     * @throws IOException - raised when writing to session failed.
     */
    public void write(final ru.mail.polis.Record record) throws IOException {
        final var key = record.getKey();
        final var keyAsBytes = ByteBufferUtils.toArray(key);
        final var value = record.getValue();
        final var parsedRecord = Record.newFromRawValue(ByteBufferUtils.toArray(value));
        if (!parsedRecord.isRemoved() && !parsedRecord.wasNotFound()) {
            final var parsedValue = parsedRecord.getValue();
            write(keyAsBytes, parsedValue);
        }
    }

    private void write(final byte[] key, final byte[] value) throws IOException {
        try (var byteStream = new ByteArrayOutputStream()) {
            final byte[] newLine = new byte[]{'\r', '\n'};
            final int chunkLength = key.length + 1 + value.length;
            final byte[] lenInfo = Integer.toHexString(chunkLength).toUpperCase()
                    .getBytes(StandardCharsets.UTF_8);
            byteStream.write(lenInfo, 0, lenInfo.length);
            byteStream.write(newLine, 0, newLine.length);
            byteStream.write(key, 0, key.length);
            byteStream.write('\n');
            byteStream.write(value, 0, value.length);
            byteStream.write(newLine, 0, newLine.length);
            final byte[] res = byteStream.toByteArray();
            logger.info("Chunked response item: " + new String(res, StandardCharsets.UTF_8));
            session.write(res, 0, res.length);
        }
    }

    /**
     * Write last chunk (0-lengthed) to session.
     *
     * @throws IOException - raised when writing to session failed.
     */
    public void close() throws IOException {
        final byte[] res = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        logger.info("Closed chunked response item: " + new String(res, StandardCharsets.UTF_8));
        session.write(res, 0, res.length);
        session.close();
    }
}
