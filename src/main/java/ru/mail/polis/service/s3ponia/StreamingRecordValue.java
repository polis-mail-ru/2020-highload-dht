package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.nio.ByteBuffer;

public class StreamingRecordValue implements StreamingValue {
    private final Record record;

    public StreamingRecordValue(@NotNull final Record record) {
        this.record = record;
    }

    @Override
    public int valueSize() {
        return record.getKey().limit() + 1 /*NEW LINE*/ + record.getValue().limit();
    }

    @Override
    public void value(@NotNull final ByteBuffer out) {
        out
                .put(record.getKey())
                .put((byte) '\n')
                .put(record.getValue());
    }
}
