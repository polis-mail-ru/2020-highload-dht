package ru.mail.polis.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DaoExpireTest extends TestBase {
    @Test
    void insert(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            final ByteBuffer value = randomValueBuffer();
            final ByteBuffer key = randomKeyBuffer();

            dao.upsert(key, value, 1_000);

            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void deadInsert(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            final ByteBuffer value = randomValueBuffer();
            final ByteBuffer key = randomKeyBuffer();

            dao.upsert(key, value, 0);

            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void compact(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            final ByteBuffer value = randomValueBuffer();
            final ByteBuffer key = randomKeyBuffer();

            dao.upsert(key, value, 1_000);

            assertEquals(value, dao.get(key));
            dao.compact();
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void deadCompact(@TempDir File data) throws IOException {
        try (DAO dao = DAOFactory.create(data)) {
            final ByteBuffer value = randomValueBuffer();
            final ByteBuffer key = randomKeyBuffer();

            dao.upsert(key, value, 0);

            assertThrows(NoSuchElementException.class, () -> dao.get(key));
            dao.compact();
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void multiple(@TempDir File data) throws IOException, InterruptedException {
        final int keyCount = 20;
        final List<ByteBuffer> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(randomKeyBuffer());
        }

        try (DAO dao = DAOFactory.create(data)) {
            for (int round = 0; round < keyCount; round++) {
                final ByteBuffer value = randomValueBuffer();
                final long expire = 1_000;

                for (final ByteBuffer key : keys) {
                    dao.upsert(key, value, expire);
                }

                for (final ByteBuffer key : keys) {
                    assertEquals(value, dao.get(key));
                }
            }

            Thread.sleep(1_000);
            dao.compact();
            for (final ByteBuffer key : keys) {
                assertThrows(NoSuchElementException.class, () -> dao.get(key));
            }
        }
    }
}
