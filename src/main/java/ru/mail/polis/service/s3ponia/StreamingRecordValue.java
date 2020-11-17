package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.util.Utility;

import java.nio.ByteBuffer;

public class StreamingRecordValue implements StreamingValue {
    private final Record record;

    public StreamingRecordValue(@NotNull final Record record) {
        this.record = record;
    }

    @Override
    public byte[] value() {
        final var key = Utility.fromByteBuffer(record.getKey());
        final var value = Utility.fromByteBuffer(record.getValue());
        final byte[] res = new byte[key.length + 1 /*NEW LINE*/ + value.length];
        ByteBuffer.wrap(res)
                .put(key)
                .put((byte) '\n')
                .put(value);
        return res;
    }
}
