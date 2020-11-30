package ru.mail.polis.expire.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.TestBase;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExpireRecordsTest extends TestBase {

    @Test
    void validExpire(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().plus(Duration.ofMinutes(5));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void validExpireCompact(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().plus(Duration.ofMinutes(5));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertEquals(value, dao.get(key));
            dao.compact();
            assertEquals(value, dao.get(key));
        }
    }

    @Test
    void nonValidExpire(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().minus(Duration.ofMinutes(5));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void nonValidExpireCompact(@TempDir File data) throws IOException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().minus(Duration.ofMinutes(5));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
            dao.compact();
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void waitForExpire(@TempDir File data) throws IOException, InterruptedException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().plus(Duration.ofSeconds(10));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertEquals(value, dao.get(key));
            Thread.sleep(10000);
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void waitForExpireCompact(@TempDir File data) throws IOException, InterruptedException {
        final ByteBuffer key = randomKeyBuffer();
        final ByteBuffer value = randomValueBuffer();
        final Instant current = Instant.now().plus(Duration.ofSeconds(10));
        final Instant expire = Instant.ofEpochSecond(current.getEpochSecond(), current.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key, value, expire);
            assertEquals(value, dao.get(key));
            dao.compact();
            assertEquals(value, dao.get(key));
            Thread.sleep(10000);
            dao.compact();
            assertThrows(NoSuchElementException.class, () -> dao.get(key));
        }
    }

    @Test
    void differentExpire(@TempDir File data) throws IOException, InterruptedException {
        final ByteBuffer key1 = randomKeyBuffer();
        final ByteBuffer value1 = randomValueBuffer();
        final ByteBuffer key2 = randomKeyBuffer();
        final ByteBuffer value2 = randomValueBuffer();
        final Instant current1 = Instant.now().plus(Duration.ofSeconds(10));
        final Instant current2 = Instant.now().plus(Duration.ofMinutes(1));
        final Instant expire1 = Instant.ofEpochSecond(current1.getEpochSecond(), current1.getNano());
        final Instant expire2 = Instant.ofEpochSecond(current2.getEpochSecond(), current2.getNano());
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(key1, value1, expire1);
            assertEquals(value1, dao.get(key1));
            dao.upsert(key2, value2, expire2);
            assertEquals(value2, dao.get(key2));
            Thread.sleep(10000);
            dao.compact();
            assertEquals(value2, dao.get(key2));
            assertThrows(NoSuchElementException.class, () -> dao.get(key1));
        }
    }

    @Test
    void multipleExpire(@TempDir File data) throws IOException, InterruptedException {
        final int keyCount = 10;

        final Collection<ByteBuffer> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(randomKeyBuffer());
        }

        try (DAO dao = DAOFactory.create(data)) {
            for (int round = 0; round < keyCount; round++) {
                final ByteBuffer value = randomValueBuffer();
                final Instant expire = randomExpire();

                for (final ByteBuffer key : keys) {
                    dao.upsert(key, value, expire);
                }

                dao.compact();
                for (final ByteBuffer key : keys) {
                    assertEquals(value, dao.get(key));
                }
            }

            Thread.sleep(31000);
            dao.compact();
            for (final ByteBuffer key : keys) {
                assertThrows(NoSuchElementException.class, () -> dao.get(key));
            }
        }
    }

}
