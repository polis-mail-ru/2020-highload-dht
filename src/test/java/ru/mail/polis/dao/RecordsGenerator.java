package ru.mail.polis.dao;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Generates {@link Record}s for test purposes.
 */
class RecordsGenerator implements Iterator<Record> {
    private final int count;
    private final int samplePeriod;

    private final Random keys;
    private final Random values;
    private final Map<Integer, Byte> samples;

    private int i;

    RecordsGenerator(final int count, final int samplePeriod) {
        this.count = count;
        this.samplePeriod = samplePeriod;
        final long keySeed = System.currentTimeMillis();
        final long valueSeed = new Random(keySeed).nextLong();
        this.keys = new Random(keySeed);
        this.values = new Random(valueSeed);
        this.samples = samplePeriod > 0 ? new HashMap<>(count / samplePeriod) : null;
        this.i = 0;
    }

    @Override
    public boolean hasNext() {
        return i < count;
    }

    @Override
    public Record next() {
        i++;

        final int keyPayload = keys.nextInt();
        final ByteBuffer key = ByteBuffer.allocate(Integer.BYTES);
        key.putInt(keyPayload);
        key.rewind();

        final byte valuePayload = (byte) values.nextInt();
        final ByteBuffer value = ByteBuffer.allocate(Byte.BYTES);
        value.put(valuePayload);
        value.rewind();

        // store the latest value by key or update previously stored one
        if (samples != null) {
            if (i % samplePeriod == 0 ||
                    samples.containsKey(keyPayload)) {
                samples.put(keyPayload, valuePayload);
            }
        }

        return Record.of(key, value);
    }

    Map<Integer, Byte> getSamples() {
        return Collections.unmodifiableMap(samples);
    }
}

