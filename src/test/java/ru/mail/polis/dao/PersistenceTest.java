/*
 * Copyright 2020 (c) Odnoklassniki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis.dao;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.Files;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence tests for {@link DAO} implementations.
 *
 * @author Vadim Tsesko
 */
class PersistenceTest extends TestBase {
    @Test
    void fs(@TempDir File data) throws IOException {
        // Reference key
        final ByteBuffer key = randomKeyBuffer();

        // Create, fill and remove storage
        try {
            try (DAO dao = DAOFactory.create(data)) {
                final ByteBuffer value = randomValueBuffer();
                dao.upsert(key, value);
                assertEquals(value, dao.get(key));
            }
        } finally {
            Files.recursiveDelete(data);
        }

        // Check that the storage is empty
        assertFalse(data.exists());
        assertTrue(data.mkdir());
        try (DAO dao = DAOFactory.create(data)) {
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void reopen(@TempDir File data) throws IOException {
        // Reference value
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();

        // Create, fill and close storage
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value);
            assertEquals(value, dao.get(key));
        }

        // Recreate dao
        try (DAO dao = DAOFactory.create(data)) {
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void remove(@TempDir File data) throws IOException {
        // Reference value
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();

        // Create dao and fill data
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value);
            assertEquals(value, dao.get(key));
        }

        // Load data and check
        try (DAO dao = DAOFactory.create(data)) {
            assertEquals(value, dao.get(key));

            // Remove data and flush
            dao.remove(key);
        }

        // Load and check not found
        try (DAO dao = DAOFactory.create(data)) {
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @RepeatedTest(1000)
    void replaceWithClose(@TempDir File data) throws Exception {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final ByteBuffer value2 = randomValueBuffer();

        // Initial insert
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value);
            assertEquals(value, dao.get(key));
        }

        // Reopen
        try (DAO dao = DAOFactory.create(data)) {
            // Check and replace
            assertEquals(value, dao.get(key));
            dao.upsert(key, value2);
            assertEquals(value2, dao.get(key));
        }

        // Reopen
        try (DAO dao = DAOFactory.create(data)) {
            // Last value should win
            assertEquals(value2, dao.get(key));
        }
    }

    @Test
    void hugeKeys(@TempDir File data) throws IOException {
        // Reference key
        final int size = 1024 * 1024;
        final ByteBuffer suffix = randomBuffer(size);
        final ByteBuffer value = randomValueBuffer();
        final int records = (int) (DAOFactory.MAX_HEAP / size + 1);
        final Collection<ByteBuffer> keys = new ArrayList<>(records);

        // Create, fill and close storage
        try (DAO dao = DAOFactory.create(data)) {
            for (int i = 0; i < records; i++) {
                final ByteBuffer key = randomKeyBuffer();
                keys.add(key);
                ByteBuffer suffixed = null;
                try {
                    suffixed = join(key, suffix);
                } catch (OutOfMemoryError e) {
                    System.out.println(e);
                }
                dao.upsert(suffixed, value);
                assertEquals(value, dao.get(suffixed));
            }
        }

        // Recreate dao and check contents
        try (DAO dao = DAOFactory.create(data)) {
            for (final ByteBuffer key : keys) {
                assertEquals(value, dao.get(join(key, suffix)));
            }
        }
    }

    @Test
    void hugeValues(@TempDir File data) throws IOException {
        // Reference value
        final int size = 1024 * 1024;
        final ByteBuffer suffix = randomBuffer(size);
        final int records = (int) (DAOFactory.MAX_HEAP / size + 1);
        final Collection<ByteBuffer> keys = new ArrayList<>(records);

        // Create, fill and close storage
        try (DAO dao = DAOFactory.create(data)) {
            for (int i = 0; i < records; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final ByteBuffer value = join(key, suffix);
                keys.add(key);
                dao.upsert(key, value);
                assertEquals(value, dao.get(key));
            }
        }

        // Recreate dao and check contents
        try (DAO dao = DAOFactory.create(data)) {
            for (final ByteBuffer key : keys) {
                assertEquals(join(key, suffix), dao.get(key));
            }
        }
    }

    @Test
    void manyRecords(@TempDir File data) throws IOException {
        // Records
        final int records = 1_000_000;
        final int sampleCount = records / 1000;

        final long keySeed = System.currentTimeMillis();
        final long valueSeed = new Random(keySeed).nextLong();

        final Random keys = new Random(keySeed);
        final Random values = new Random(valueSeed);
        final Map<Integer, Byte> samples = new HashMap<>(sampleCount);

        try (final DAO dao = DAOFactory.create(data)) {
            // Populate (LSM is fast for writes)
            for (int i = 0; i < records; i++) {
                final int keyPayload = keys.nextInt();
                final ByteBuffer key = ByteBuffer.allocate(Integer.BYTES);
                key.putInt(keyPayload);
                key.rewind();

                final byte valuePayload = (byte) values.nextInt();
                final ByteBuffer value = ByteBuffer.allocate(Byte.BYTES);
                value.put(valuePayload);
                value.rewind();

                dao.upsert(key, value);

                // store the latest value by key or update previously stored one
                if (i % sampleCount == 0 ||
                        samples.containsKey(keyPayload)) {
                    samples.put(keyPayload, valuePayload);
                    assertEquals(value, dao.get(key));
                }
            }

            // Check the contents with sampling (LSM is slow for reads)
            for (final Map.Entry<Integer, Byte> sample : samples.entrySet()) {
                final ByteBuffer key = ByteBuffer.allocate(Integer.BYTES);
                key.putInt(sample.getKey());
                key.rewind();

                final ByteBuffer value = ByteBuffer.allocate(Byte.BYTES);
                value.put(sample.getValue());
                value.rewind();

                assertEquals(value, dao.get(key));
            }
        }
    }

    @Test
    void burn(@TempDir File data) throws IOException {
        // Fixed key
        final ByteBuffer key = randomKeyBuffer();

        // Overwrite key multiple times
        final int overwrites = 100;
        for (int i = 0; i < overwrites; i++) {
            // Overwrite
            final ByteBuffer value = randomValueBuffer();
            try (DAO dao = DAOFactory.create(data)) {
                dao.upsert(key, value);
                assertEquals(value, dao.get(key));
            }

            // Check
            try (DAO dao = DAOFactory.create(data)) {
                assertEquals(value, dao.get(key));
            }
        }
    }
}
