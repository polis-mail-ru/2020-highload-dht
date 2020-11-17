package ru.mail.polis.service.nik27090;

import ru.mail.polis.Record;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ChunkIterator implements Iterator<byte[]> {

    private final static byte[] SEP = "\n".getBytes(StandardCharsets.UTF_8);
    private final static byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private final static byte[] END = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    private final Iterator<Record> records;

    public ChunkIterator(final Iterator<Record> records) {
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
        final int dataLength = key.remaining() + value.remaining() + SEP.length;
        final byte[] data = toByteArray(ByteBuffer.allocate(dataLength).put(key).put(SEP).put(value).position(0));
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

    /**
     * Convert ByteBuffer to byte[].
     *
     * @param buffer - byteBuffer
     * @return - byte[]
     */
    public static byte[] toByteArray(final ByteBuffer buffer) {
        final var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    public byte[] end() {
        return END.clone();
    }
}
