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
import org.opentest4j.AssertionFailedError;
import ru.mail.polis.Record;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Generates a lot of tombstones
 * and ensures that resulted DAO is empty.
 *
 * @author Dmitry Schitinin
 */
class AllDeadTest extends TestBase {

    private static final int TOMBSTONES_COUNT = 1000000;

    @Test
    void deadAll(@TempDir File data) throws IOException {
        // Create, fill, read and remove
        try (DAO dao = DAOFactory.create(data)) {
            final Iterator<ByteBuffer> tombstones =
                    Stream.generate(TestBase::randomKeyBuffer)
                            .limit(TOMBSTONES_COUNT)
                            .iterator();
            while (tombstones.hasNext()) {
                try {
                    dao.remove(tombstones.next());
                } catch (IOException e) {
                    throw new AssertionFailedError("Unable to remove");
                }
            }

            // Check contents
            final Iterator<Record> empty = dao.iterator(ByteBuffer.allocate(0));
            assertFalse(empty.hasNext());
        }
    }
}
