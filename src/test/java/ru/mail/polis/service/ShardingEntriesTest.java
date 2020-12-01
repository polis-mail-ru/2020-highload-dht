package ru.mail.polis.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.google.common.primitives.Bytes;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Record;

class ShardingEntriesTest extends ShardingTest {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static final byte[] EOL = "\n".getBytes(StandardCharsets.UTF_8);
    private static List<Chunk> chunks;

    @BeforeEach
    void beforeAll() {
        chunks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final String key = randomId();
            final byte[] value = randomValue();
            final Chunk chunk = new Chunk(key, value);
            chunks.add(chunk);
            assertTimeoutPreemptively(TIMEOUT,
                    () -> assertEquals(201, upsert(0, key, value, 1, 1).getStatus()));
        }
        chunks.sort(Chunk::compareTo);
    }

    @Test
    void rangeEntriesTest() {
        final Random random = new Random();
        int splitIdx = 0;
        while (splitIdx <= 0 || splitIdx > chunks.size()) {
            splitIdx = random.nextInt(chunks.size() / 3);
        }
        int splitTo = chunks.size() - splitIdx;
        final Chunk rangeFrom = chunks.get(splitIdx);
        final Chunk rangeTo = chunks.get(splitTo);
        final List<Chunk> expectedChunks = chunks.subList(splitIdx, splitTo);
        final List<byte[]> chunksBytes = expectedChunks.stream().map(Chunk::getBytes).collect(Collectors.toList());
        final ByteBuffer buffer = ByteBuffer.allocate(
                chunksBytes.stream().map(it -> it.length).reduce(0, Integer::sum));
        for (final byte[] chunkBytes : chunksBytes) {
            buffer.put(chunkBytes);
        }

        final byte[] expectedBytes = buffer.array();
        for (int i = 0; i < getClusterSize(); i++) {
            int node = i;
            assertTimeoutPreemptively(TIMEOUT, () -> {
                final Response response = range(node, rangeFrom.key, rangeTo.key);
                assertEquals(200, response.getStatus());
                final byte[] body = response.getBody();
                assertArrayEquals(expectedBytes, body);
            });
        }
    }

    private static class Chunk implements Comparable<Chunk> {

        private final String key;
        private final Record record;
        private final byte[] bytes;

        Chunk(final String key, final byte[] value) {
            this.key = key;
            this.record = Record.of(ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(value));
            this.bytes = Bytes.concat(key.getBytes(StandardCharsets.UTF_8), EOL, value);
        }

        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public int compareTo(@NotNull Chunk chunk) {
            return record.compareTo(chunk.record);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Chunk)) {
                return false;
            }
            Chunk chunk = (Chunk) object;
            return key.equals(chunk.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
