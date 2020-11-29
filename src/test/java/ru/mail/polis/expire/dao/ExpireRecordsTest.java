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

}
