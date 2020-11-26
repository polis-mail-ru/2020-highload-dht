package ru.mail.polis.service.s3ponia;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;

public class StreamingRecordValue implements StreamingValue {
    private static final byte[] CARRIAGE_RETURN_LINE_FEED =
            "\r\n".getBytes(Charsets.UTF_8);
    private final ByteBuffer key;
    private final ByteBuffer value;

    public StreamingRecordValue(@NotNull final Record record) {
        this.key = record.getKey();
        this.value = record.getValue();
    }

    private int size() {
        return this.key.limit() + 1 /* NEW LINE */ + this.value.limit();
    }

    @Override
    public byte[] value() {
        final var sendSize = size();
        final var stringSize = Integer.toHexString(sendSize);
        final var chunkSize = stringSize.length() + CARRIAGE_RETURN_LINE_FEED.length
                + sendSize + CARRIAGE_RETURN_LINE_FEED.length;
        final var chunk = ByteBuffer.allocate(chunkSize);
        chunk.put(stringSize.getBytes(Charsets.UTF_8))
                .put(CARRIAGE_RETURN_LINE_FEED)
                .put(this.key)
                .put((byte) '\n')
                .put(this.value)
                .put(CARRIAGE_RETURN_LINE_FEED);
        return chunk.array();
    }
}
