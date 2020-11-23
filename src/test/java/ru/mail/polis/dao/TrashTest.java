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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.TestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks ignoring trash files in persistent data directory.
 *
 * @author Dmitry Schitinin
 */
class TrashTest extends TestBase {
    @Test
    void ignoreEmptyTrashFiles(@TempDir File data) throws IOException {
        // Reference value
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant expire = randomExpire();

        // Create dao and fill data
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
        }

        createTrashFile(data, "trash.txt");
        createTrashFile(data, "trash.dat");
        createTrashFile(data, "trash");
        createTrashFile(data, "trash_0");
        createTrashFile(data, "trash.db");
        createTrashFile(data, "123trash.dat");
        createTrashFile(data, "trash123.dat");

        // Load and check stored value
        try (DAO dao = DAOFactory.create(data)) {
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void ignoreTrashDirectories(@TempDir File data) throws IOException {
        // Reference value
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant expire = randomExpire();

        // Create dao and fill data
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
        }

        createTrashDirectory(data, "trash.txt");
        createTrashDirectory(data, "trash.dat");
        createTrashDirectory(data, "trash");
        createTrashDirectory(data, "trash_0");
        createTrashDirectory(data, "trash.db");

        // Load and check stored value
        try (DAO dao = DAOFactory.create(data)) {
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void ignoreNonEmptyTrashFiles(@TempDir File data) throws IOException {
        // Reference value
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant expire = randomExpire();

        // Create dao and fill data
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
        }

        createTrashFile(data, "trash.txt", randomValueBuffer());
        createTrashFile(data, "trash.dat", randomValueBuffer());
        createTrashFile(data, "trash", randomValueBuffer());
        createTrashFile(data, "trash_0", randomValueBuffer());
        createTrashFile(data, "trash.db", randomValueBuffer());

        // Load and check stored value
        try (DAO dao = DAOFactory.create(data)) {
            assertEquals(value, dao.get(key));
        }
    }

    private static void createTrashFile(
            final File dir,
            final String name) throws IOException {
        assertTrue(new File(dir, name).createNewFile());
    }

    private static void createTrashDirectory(
            final File dir,
            final String name) {
        assertTrue(new File(dir, name).mkdir());
    }

    private static void createTrashFile(
            final File dir,
            final String name,
            final ByteBuffer content) throws IOException {
        try (final FileChannel ch =
                     FileChannel.open(
                             Paths.get(dir.getAbsolutePath(), name),
                             StandardOpenOption.CREATE,
                             StandardOpenOption.WRITE)) {
            ch.write(content);
        }
    }
}
