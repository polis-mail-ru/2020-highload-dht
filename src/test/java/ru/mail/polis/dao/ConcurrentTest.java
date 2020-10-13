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
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.mail.polis.Record;
import ru.mail.polis.TestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concurrency tests for {@link DAO}.
 */
class ConcurrentTest extends TestBase {

    @Test
    void singleWriter(@TempDir File data) throws IOException, InterruptedException {
        concurrentWrites(1, 1000, 1, data);
    }

    @Test
    void twoWriters(@TempDir File data) throws IOException, InterruptedException {
        concurrentWrites(2, 1000, 1, data);
    }

    @Test
    void twoWritersManyRecords(@TempDir File data) throws IOException, InterruptedException {
        concurrentWrites(2, 1_000_000, 1000, data);
    }

    @Test
    void tenWritersManyRecords(@TempDir File data) throws IOException, InterruptedException {
        concurrentWrites(10, 1_000_000, 1000, data);
    }

    @Test
    void singleReaderWriter(@TempDir File data) throws IOException, InterruptedException {
        concurrentReadWrite(1, 1_000,  data);
    }

    @Test
    void twoReaderWriter(@TempDir File data) throws IOException, InterruptedException {
        concurrentReadWrite(2, 1_000,  data);
    }

    @Test
    void tenReaderWriterManyRecords(@TempDir File data) throws IOException, InterruptedException {
        concurrentReadWrite(10, 1_000_000,  data);
    }

    private void concurrentWrites(int threadsCount,
                                  int recordsCount, int samplePeriod,
                                  @NotNull final File data)
            throws IOException, InterruptedException {
        final RecordsGenerator records = new RecordsGenerator(recordsCount, samplePeriod);
        try (final DAO dao = DAOFactory.create(data)) {
            final ExecutorService executor = new ThreadPoolExecutor(threadsCount, threadsCount,
                    1, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(1024),
                    (r, usedExecutor) -> {
                        try {
                            // test thread will be blocked and wait
                            usedExecutor.getQueue().put(r);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            while (records.hasNext()) {
                final Record record = records.next();
                executor.submit(() -> {
                    try {
                        dao.upsert(record.getKey(), record.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }

        // Recreate dao and check the contents with sampling (LSM is slow for reads)
        try (final DAO dao = DAOFactory.create(data)) {
            for (final Map.Entry<Integer, Byte> sample : records.getSamples().entrySet()) {
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

    private void concurrentReadWrite(int threadsCount,
<<<<<<< HEAD
                                     int recordsCount,
                                     @NotNull final File data)
=======
                                  int recordsCount,
                                  @NotNull final File data)
>>>>>>> bf3b4a20548990bb4253a36ce5cd4ff27b9c4a8a
            throws IOException, InterruptedException {
        final RecordsGenerator records = new RecordsGenerator(recordsCount, 0);
        try (final DAO dao = DAOFactory.create(data)) {
            final ExecutorService executor = new ThreadPoolExecutor(threadsCount, threadsCount,
                    1, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(1024),
                    (r, usedExecutor) -> {
                        try {
                            // test thread will be blocked and wait
                            usedExecutor.getQueue().put(r);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
<<<<<<< HEAD
            final AtomicInteger matches = new AtomicInteger();
=======
            final AtomicInteger matches = new AtomicInteger(); 
>>>>>>> bf3b4a20548990bb4253a36ce5cd4ff27b9c4a8a
            while (records.hasNext()) {
                final Record record = records.next();
                executor.submit(() -> {
                    try {
                        dao.upsert(record.getKey(), record.getValue());
                        ByteBuffer value = dao.get(record.getKey());
                        if (value.equals(record.getValue().duplicate().rewind())) {
                            matches.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
            assertEquals(recordsCount, matches.get());
        }
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> bf3b4a20548990bb4253a36ce5cd4ff27b9c4a8a
