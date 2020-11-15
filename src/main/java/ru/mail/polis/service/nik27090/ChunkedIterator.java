package ru.mail.polis.service.nik27090;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ChunkedIterator implements Iterator<byte[]> {

    private final byte[] SEPARATE = "\n".getBytes(StandardCharsets.UTF_8);
    private final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private final byte[] END = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    private final Iterator<Record> records;

    public ChunkedIterator(Iterator<Record> records) {
        this.records = records;
    }

    @Override
    public boolean hasNext() {
        return records.hasNext();
    }

    @Override
    public byte[] next() {
        final Record record = records.next();
        final ByteBuffer key = record.getKey();
        final ByteBuffer value = record.getValue();
        final int dataLength = key.remaining() + value.remaining() + SEPARATE.length;
        final byte[] data = toByteArray(ByteBuffer.allocate(dataLength).put(key).put(SEPARATE).put(value).position(0));
        final byte[] chunkLength = Integer.toHexString(dataLength).getBytes(StandardCharsets.UTF_8);

        return toByteArray(
                ByteBuffer.allocate(data.length + 2 * CRLF.length + chunkLength.length)
                        .put(chunkLength)
                        .put(CRLF)
                        .put(data)
                        .put(CRLF)
                        .position(0)
        );
    }

    public static byte[] toByteArray(final ByteBuffer buffer) {
        final var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    public byte[] end() {
        return END;
    }
}
