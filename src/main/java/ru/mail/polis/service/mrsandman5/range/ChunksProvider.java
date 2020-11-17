package ru.mail.polis.service.mrsandman5.range;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.utils.ByteUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class ChunksProvider {

    private static final byte[] eol = "\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] crlf = "\r\n".getBytes(Charsets.US_ASCII);
    private static final byte[] eof = "0\r\n\r\n".getBytes(Charsets.US_ASCII);

    private final Iterator<Record> records;

    /**
     * Wrapper over iterator to get chunks.
     *
     * @param records Record iterator.
     */
    ChunksProvider(@NotNull final Iterator<Record> records) {
        this.records = records;
    }

    /**
     * Get next chunk.
     *
     * @return byte array of chunk.
     */
    byte[] next() {
        final Record record = records.next();
        final byte[] key = ByteUtils.toByteArray(record.getKey());
        final byte[] value = ByteUtils.toByteArray(record.getValue());

        final int payloadLength = key.length + 1 + value.length;
        final String chunkHexSize = Integer.toHexString(payloadLength);
        final byte[] chunk = new byte[chunkHexSize.length() + 2 * crlf.length + payloadLength];
        ByteBuffer.wrap(chunk)
                .put(chunkHexSize.getBytes(StandardCharsets.US_ASCII))
                .put(crlf)
                .put(key)
                .put(eol)
                .put(value)
                .put(crlf);
        return chunk;
    }

    boolean hasNext() {
        return records.hasNext();
    }

    /**
     * Get last chunk.
     *
     * @return byte array last chunk.
     */
    byte[] end() {
        return Arrays.copyOf(eof, eof.length);
    }
}
