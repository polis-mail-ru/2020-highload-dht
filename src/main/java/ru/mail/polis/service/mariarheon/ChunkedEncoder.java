package ru.mail.polis.service.mariarheon;

import ru.mail.polis.dao.mariarheon.ByteBufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChunkedEncoder {
    final ByteArrayOutputStream outputStream;
    // final boolean headerWasWritten;

    public ChunkedEncoder() {
        outputStream = new ByteArrayOutputStream();
        // headerWasWritten =
    }

    /*
    public void foo() {
        byte[] bytes = response.toBytes(includeBody);
        session.write(bytes, 0, bytes.length);
    }
     */

    public void add(ru.mail.polis.Record record) {
        final var key = record.getKey();
        final var keyAsBytes = ByteBufferUtils.toArray(key);
        final var value = record.getValue();
        final var parsedRecord = Record.newFromRawValue(ByteBufferUtils.toArray(value));
        if (!parsedRecord.isRemoved() && !parsedRecord.wasNotFound()) {
            final var parsedValue = parsedRecord.getValue();
            add(keyAsBytes, parsedValue);
        }
    }

    private void add(final byte[] key, final byte[] value) {
        try {
            outputStream.write(key);
            outputStream.write('\n');
            outputStream.write(value);
        } catch (IOException e) {
            /* this exception cannot be thrown */
        }
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}
