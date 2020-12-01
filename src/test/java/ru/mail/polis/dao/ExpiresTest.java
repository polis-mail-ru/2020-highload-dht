package ru.mail.polis.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class ExpiresTest extends TestBase {

    private static final long LIFE_TIME_DELTA_MILLIS = 500L;
    private static final long DELAY = LIFE_TIME_DELTA_MILLIS * 2L;

    @Test
    void recordMustExpire(@TempDir final File data) throws IOException, InterruptedException {
        final ByteBuffer keyBuffer = randomKeyBuffer();
        final ByteBuffer valueBuffer = randomValueBuffer();
        final long expiresTimestamp = System.currentTimeMillis() + LIFE_TIME_DELTA_MILLIS;

        try (final DAO dao = DAOFactory.create(data)) {
            dao.upsert(keyBuffer, valueBuffer, expiresTimestamp);
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer);
            Thread.sleep(DELAY);
            Assertions.assertThrows(NoSuchElementException.class, () -> dao.get(keyBuffer));
        }
    }

    @Test
    void overwrittenRecordMustExpire(@TempDir final File data) throws IOException, InterruptedException {
        final ByteBuffer keyBuffer = randomKeyBuffer();
        final ByteBuffer valueBuffer1 = randomValueBuffer();
        final ByteBuffer valueBuffer2 = randomValueBuffer();
        final long expiresTimestamp = System.currentTimeMillis() + LIFE_TIME_DELTA_MILLIS;

        try (final DAO dao = DAOFactory.create(data)) {
            // upsert never-expires record with key and value1,
            dao.upsert(keyBuffer, valueBuffer1);
            // check that upsert is successful
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer1);
            // overwrite key-record with value2 and expiresTimestamp
            dao.upsert(keyBuffer, valueBuffer2, expiresTimestamp);
            // check that overwrite is successful
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer2);
            // sleep DELAY (DELAY = 2 * LIFE_TIME_DELTA)
            Thread.sleep(DELAY);
            // assert that our record has expired
            Assertions.assertThrows(NoSuchElementException.class, () -> dao.get(keyBuffer));
        }
    }

    @Test
    void overwrittenExpireRecordMustNotExpire(@TempDir final File data) throws IOException, InterruptedException {
        final ByteBuffer keyBuffer = randomKeyBuffer();
        final ByteBuffer valueBuffer1 = randomValueBuffer();
        final ByteBuffer valueBuffer2 = randomValueBuffer();
        final long expiresTimestamp = System.currentTimeMillis() + LIFE_TIME_DELTA_MILLIS;

        try (final DAO dao = DAOFactory.create(data)) {
            // upsert record with key, value1, expiresTimestamp
            dao.upsert(keyBuffer, valueBuffer1, expiresTimestamp);
            // check that upsert is successful
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer1);
            // overwrite key-record with value2
            dao.upsert(keyBuffer, valueBuffer2);
            // check that overwrite is successful
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer2);
            // sleep DELAY (DELAY = 2 * LIFE_TIME_DELTA)
            Thread.sleep(DELAY);
            // assert that our record hasn't expired
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer2);
        }
    }

    @Test
    void expireSerializerIsCorrect(@TempDir final File data) throws IOException, InterruptedException {
        final ByteBuffer keyBuffer = randomKeyBuffer();
        final ByteBuffer valueBuffer = randomValueBuffer();
        final long expiresTimestamp = System.currentTimeMillis() + LIFE_TIME_DELTA_MILLIS;

        try (final DAO dao = DAOFactory.create(data)) {
            // upsert record with key, value1, expiresTimestamp
            dao.upsert(keyBuffer, valueBuffer, expiresTimestamp);
            // check that upsert is successful
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer);
        }

        try (final DAO dao = DAOFactory.create(data)) {
            Assertions.assertTrue(
                    System.currentTimeMillis() < expiresTimestamp,
                    "Your storage is so slow. Please, increase LIFE_TIME_DELTA variable."
            );
            // check that data is saved
            Assertions.assertEquals(dao.get(keyBuffer), valueBuffer);
            Assertions.assertEquals(dao.rowGet(keyBuffer).getExpiresTimestamp(), expiresTimestamp);

            Thread.sleep(DELAY);
            // assert that our record has expired
            Assertions.assertThrows(NoSuchElementException.class, () -> dao.get(keyBuffer));
        }
    }
}
