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

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.Record;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional unit tests for {@link DAO} implementations.
 *
 * @author Vadim Tsesko
 */
class BasicTest extends TestBase {
    @Test
    void empty(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            assertThrows(NoSuchElementException.class, () -> dao.get(randomKeyBuffer()));
        }
    }

    @Test
    void insert(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, null);
            assertEquals(value, dao.get(key));
            assertEquals(value, dao.get(key.duplicate()));
        }
    }

    @Test
    void fullScan(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            // Generate and insert data
            final int count = 10;
            final NavigableMap<ByteBuffer, ByteBuffer> map = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final ByteBuffer value = randomValueBuffer();
                dao.upsert(key, value, null);
                assertNull(map.put(key, value));
            }

            // Check the data
            final Iterator<Map.Entry<ByteBuffer, ByteBuffer>> expectedIter = map.entrySet().iterator();
            final Iterator<Record> actualIter = dao.iterator(ByteBuffer.wrap(new byte[0]));
            while (expectedIter.hasNext()) {
                final Map.Entry<ByteBuffer, ByteBuffer> expected = expectedIter.next();
                final Record actual = actualIter.next();
                final ByteBuffer expectedKey = expected.getKey();
                final ByteBuffer actualKey = actual.getKey();
                assertEquals(expectedKey, actualKey);
                assertEquals(expected.getValue(), actual.getValue());
            }
            assertFalse(actualIter.hasNext());
        }
    }

    @Test
    void firstScan(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            // Generate and insert data
            final int count = 10;
            final NavigableMap<ByteBuffer, ByteBuffer> map = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final ByteBuffer value = randomValueBuffer();
                dao.upsert(key, value, null);
                assertNull(map.put(key, value));
            }

            // Check the data
            final Iterator<Map.Entry<ByteBuffer, ByteBuffer>> expectedIter = map.entrySet().iterator();
            final Iterator<Record> actualIter = dao.iterator(map.firstKey());
            while (expectedIter.hasNext()) {
                final Map.Entry<ByteBuffer, ByteBuffer> expected = expectedIter.next();
                final Record actual = actualIter.next();
                final ByteBuffer expectedKey = expected.getKey();
                final ByteBuffer actualKey = actual.getKey();
                assertEquals(expectedKey, actualKey);
                assertEquals(expected.getValue(), actual.getValue());
            }
            assertFalse(actualIter.hasNext());
        }
    }

    @Test
    void middleScan(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            // Generate and insert data
            final int count = 10;
            final NavigableMap<ByteBuffer, ByteBuffer> map = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final ByteBuffer value = randomValueBuffer();
                dao.upsert(key, value, null);
                assertNull(map.put(key, value));
            }

            // Check the data
            final ByteBuffer middle = Iterators.get(map.keySet().iterator(), count / 2);
            final Iterator<Map.Entry<ByteBuffer, ByteBuffer>> expectedIter =
                    map.tailMap(middle).entrySet().iterator();
            final Iterator<Record> actualIter = dao.iterator(middle);
            while (expectedIter.hasNext()) {
                final Map.Entry<ByteBuffer, ByteBuffer> expected = expectedIter.next();
                final Record actual = actualIter.next();
                final ByteBuffer expectedKey = expected.getKey();
                final ByteBuffer actualKey = actual.getKey();
                assertEquals(expectedKey, actualKey);
                assertEquals(expected.getValue(), actual.getValue());
            }
            assertFalse(actualIter.hasNext());
        }
    }

    @Test
    void rightScan(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            // Generate and insert data
            final int count = 10;
            final NavigableMap<ByteBuffer, ByteBuffer> map = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                final ByteBuffer key = randomKeyBuffer();
                final ByteBuffer value = randomValueBuffer();
                dao.upsert(key, value, null);
                assertNull(map.put(key, value));
            }

            // Check the data
            final Iterator<Record> actualIter = dao.iterator(map.lastKey());
            assertEquals(map.get(map.lastKey()), actualIter.next().getValue());
            assertFalse(actualIter.hasNext());
        }
    }

    @Test
    void emptyValue(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = ByteBuffer.allocate(0);
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, null);
            assertEquals(value, dao.get(key));
            assertEquals(value, dao.get(key.duplicate()));
        }
    }

    @Test
    void upsert(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value1 = randomValueBuffer();
        final ByteBuffer value2 = randomValueBuffer();
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value1, null);
            assertEquals(value1, dao.get(key));
            assertEquals(value1, dao.get(key.duplicate()));
            dao.upsert(key, value2, null);
            assertEquals(value2, dao.get(key));
            assertEquals(value2, dao.get(key.duplicate()));
        }
    }

    @Test
    void remove(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, null);
            assertEquals(value, dao.get(key));
            assertEquals(value, dao.get(key.duplicate()));
            dao.remove(key);
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void removeAbsent(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        try (DAO dao = DAOFactory.create(data)) {
            dao.remove(key);
        }
    }
}
