package ru.mail.polis.service.mrsandman5.range;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

public class ChunksProvider {

    private static final byte NEW_LINE = '\n';
    private static final byte[] SEPARATOR = "\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] EMPTY_CHUNK = "0\r\n\r\n".getBytes(Charsets.UTF_8);

    private final Iterator<Record> records;

    /**
     * Wrapper over iterator to get chunks.
     *
     * @param records Record iterator.
     */
    public ChunksProvider(@NotNull final Iterator<Record> records) {
        this.records = records;
    }

    /**
     * Get next chunk.
     *
     * @return byte array of chunk.
     */
    public byte[] next() {
        assert hasNext();
        final Record record = records.next();
        final ByteBuffer key = record.getKey();
        final ByteBuffer value = record.getValue();

        final int payloadLength = key.remaining() + 1 + value.remaining();
        final String chunkHexSize = Integer.toHexString(payloadLength);
        final int chunkLength = chunkHexSize.length() + 2 + payloadLength + 2;

        final byte[] chunk = new byte[chunkLength];
        ByteBuffer.wrap(chunk)
                .put(chunkHexSize.getBytes(Charsets.UTF_8))
                .put(SEPARATOR)
                .put(key)
                .put(NEW_LINE)
                .put(value)
                .put(SEPARATOR);
        return chunk;
    }

    public boolean hasNext() {
        return records.hasNext();
    }

    /**
     * Get last chunk.
     *
     * @return byte array last chunk.
     */
    public byte[] end() {
        return Arrays.copyOf(EMPTY_CHUNK, EMPTY_CHUNK.length);
    }
}
