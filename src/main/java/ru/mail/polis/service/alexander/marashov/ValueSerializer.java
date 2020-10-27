package ru.mail.polis.service.alexander.marashov;

import one.nio.serial.CalcSizeStream;
import one.nio.serial.DeserializeStream;
import one.nio.serial.SerializeStream;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class ValueSerializer {

    private ValueSerializer() {

    }

    public static byte[] serialize(final Value value) throws IOException {
        final SerializableValue serializableValue = new SerializableValue(value);
        final CalcSizeStream css = new CalcSizeStream();
        css.writeObject(serializableValue);
        final int length = css.count();

        final byte[] buf = new byte[length];
        final SerializeStream out = new SerializeStream(buf);
        out.writeObject(serializableValue);
        return buf;
    }

    public static Value deserialize(final byte[] buffer) throws ClassNotFoundException, IOException {
        final DeserializeStream in = new DeserializeStream(buffer);
        final SerializableValue serializableValue = (SerializableValue) in.readObject();

        final Value value;
        if (serializableValue.value == null) {
            value = new Value(serializableValue.timestamp, null);
        } else {
            value = new Value(serializableValue.timestamp, ByteBuffer.wrap(serializableValue.value));
        }
        return value;
    }

    private static class SerializableValue implements Serializable {

        private static final long serialVersionUID = 1L;

        final public byte[] value;
        final public long timestamp;

        public SerializableValue(final Value value) {
            this.timestamp = value.getTimestamp();
            if (value.isTombstone()) {
                this.value = null;
            } else {
                this.value = value.getData().array();
            }
        }
    }
}
