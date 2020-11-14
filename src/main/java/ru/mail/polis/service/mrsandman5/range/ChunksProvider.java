package ru.mail.polis.service.mrsandman5.range;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.utils.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class ChunksProvider {

    private final byte[] eol = "\n".getBytes(StandardCharsets.US_ASCII);
    private final byte[] crlf = "\r\n".getBytes(Charsets.US_ASCII);
    private final byte[] eof = "0\r\n\r\n".getBytes(Charsets.US_ASCII);

    final Iterator<Record> records;

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
    byte[] next() throws IOException {
        final Record record = records.next();
        final byte[] key = ByteUtils.toByteArray(record.getKey());
        final byte[] value = ByteUtils.toByteArray(record.getValue());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(key);
        outputStream.write(eol);
        outputStream.write(value);
        final byte[] data = outputStream.toByteArray();

        final byte[] chunkHexSize = Integer.toHexString(data.length).getBytes(StandardCharsets.US_ASCII);
        final byte[] chunk = new byte[chunkHexSize.length + 2 * crlf.length + data.length];

        ByteBuffer.wrap(chunk)
                .put(chunkHexSize)
                .put(crlf)
                .put(data)
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
