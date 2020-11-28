package ru.mail.polis.service.mariarheon;

import one.nio.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Encoder for writing chunks to the session in Transfer-Chunked-Encoding format.
 */
public final class ChunkedEncoder {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    private ChunkedEncoder() {
        /* nothing */
    }

    /**
     * Write new chunk with the record information to session.
     *
     * @param record - key-value pair, which should be written as chunk to session.
     * @throws IOException - raised when writing to session failed.
     */
    public static void write(final HttpSession session, final ru.mail.polis.Record record) throws IOException {
        final var value = record.getValue();
        final var parsedRecord = Record.newFromRawValue(value);
        if (!parsedRecord.isRemoved() && !parsedRecord.wasNotFound()) {
            final var parsedValue = parsedRecord.getValue();
            write(session, record.getKey(), parsedValue);
        }
    }

    private static void write(final HttpSession session,
                              final ByteBuffer key, final byte[] value) throws IOException {
        try (var byteStream = new ByteArrayOutputStream()) {
            final byte[] newLine = new byte[]{'\r', '\n'};
            final int chunkLength = key.remaining() + 1 + value.length;
            final byte[] lenInfo = Integer.toHexString(chunkLength).toUpperCase()
                    .getBytes(StandardCharsets.UTF_8);
            byteStream.write(lenInfo, 0, lenInfo.length);
            byteStream.write(newLine, 0, newLine.length);
            final byte[] keyBytes = ByteBufferUtils.toArray(key);
            byteStream.write(keyBytes, 0, keyBytes.length);
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
    public static void writeLastChunk(final HttpSession session) throws IOException {
        final byte[] res = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        logger.info("Closed chunked response item: " + new String(res, StandardCharsets.UTF_8));
        session.write(res, 0, res.length);
    }
}
