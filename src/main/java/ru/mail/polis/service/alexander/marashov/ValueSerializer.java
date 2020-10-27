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

    /**
     * Static method to serialize DAO's value to array of bytes.
     * @param value - value to serialize.
     * @return array of bytes with serialized data.
     * @throws IOException if serialize error.
     */
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

    /**
     * Static method to deserialize array of bytes to DAO's value.
     * @param buffer - array of bytes with serialized datae.
     * @return value - deserialized object.
     * @throws IOException if deserialize error.
     */
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

        public final byte[] value;
        public final long timestamp;

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
