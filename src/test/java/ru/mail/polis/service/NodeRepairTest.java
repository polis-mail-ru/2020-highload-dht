package ru.mail.polis.service;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Record;
import ru.mail.polis.util.hash.ConcatHash;
import ru.mail.polis.util.hash.TigerHash;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static ru.mail.polis.util.Utility.fromByteArray;

public class NodeRepairTest extends ClusterTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    
    @Override
    int getClusterSize() {
        return 3;
    }
    
    @Test
    void singleRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            for (int node = 0; node < getClusterSize(); node++) {
                final String key = "key" + node;
                final byte[] value = randomValue();
                stop(node);
                for (int i = (node + 1) % getClusterSize(); i != node; i = (i + 1) % getClusterSize()) {
                    assertEquals(201, upsert(i, key, value, 2, 3).getStatus());
                }
                createAndStart(node);
                assertEquals(200, repair(node).getStatus());
                
                waitForVersionAdvancement();
                
                for (int i = (node + 1) % getClusterSize(); i != node; i = (i + 1) % getClusterSize()) {
                    stop(i);
                }
                
                checkResponse(200, value, get(node, key, 1, 3));
                
                for (int i = (node + 1) % getClusterSize(); i != node; i = (i + 1) % getClusterSize()) {
                    createAndStart(i);
                }
            }
        });
    }
    
    @Test
    void allRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 1024;
            final var value = randomValue();
            final Collection<String> ids = new ArrayList<>(records);
            
            stop(0);
            for (int node = 1; node < getClusterSize(); node++) {
                for (int i = 0; i < records; i++) {
                    final var id = randomId();
                    ids.add(id);
                    assertEquals(201, upsert(node, id, value).getStatus());
                }
            }
            createAndStart(0);
            
            assertEquals(200, repair(0).getStatus());
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var id :
                    ids) {
                checkResponse(200, value, get(0, id, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void noRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 5_000;
            final var value = randomValue();
            final Collection<String> ids = new ArrayList<>(records);
            
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            assertEquals(200, repair(0).getStatus());
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var id :
                    ids) {
                checkResponse(200, value, get(0, id, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void missedValue() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 512;
            final var value = randomValue();
            final Collection<String> ids = new ArrayList<>(records);
            
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            stop(0);
            final var id = randomId();
            ids.add(id);
            assertEquals(201, upsert(1, id, value).getStatus());
            createAndStart(0);
            
            assertEquals(200, repair(0).getStatus());
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var idElement :
                    ids) {
                checkResponse(200, value, get(0, idElement, 1, getClusterSize()));
            }
        });
    }
    
    private static final ConcatHash hash = new TigerHash();
    
    private static byte[] hash(@NotNull final Record record) {
        final var byteBuffer =
                ByteBuffer
                        .allocate(
                                record.getKey().capacity()
                                        + record.getValue().capacity()
                                        + Long.BYTES /* TIMESTAMP */
                        );
        byteBuffer.put(record.getKey());
        byteBuffer.put(record.getValue());
        return hash.hash(byteBuffer.array());
    }
    
    private static long longHash(@NotNull final byte[] hashArray) {
        return fromByteArray(hashArray, 0, Long.BYTES) & Long.MAX_VALUE;
    }
    
    @Test
    void missedRange() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 512;
            final var value = randomValue();
            final Collection<String> ids = new ArrayList<>(records * 2);
            final Collection<String> missedIds = new ArrayList<>(records);
            long startMissedRange = Long.MAX_VALUE;
            long endMissedRange = 0;
            
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            stop(0);
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                missedIds.add(id);
                final var longHash =
                        longHash(hash(Record.of(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)),
                                ByteBuffer.wrap(value))));
                if (longHash < startMissedRange) {
                    startMissedRange = longHash;
                }
                if (longHash > endMissedRange) {
                    endMissedRange = longHash;
                }
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            createAndStart(0);
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            assertEquals(200, repair(0, startMissedRange, endMissedRange).getStatus());
            
            for (final var idElement :
                    ids) {
                checkResponse(200, value, get(0, idElement, 1, getClusterSize()));
            }
            
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var idElement :
                    missedIds) {
                checkResponse(200, value, get(0, idElement, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void missedDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 512;
            final var value = randomValue();
            final Collection<String> ids = new ArrayList<>(records);
            
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
    
            final var id = randomId();
            assertEquals(201, upsert(1, id, value).getStatus());
            
            stop(0);
            assertEquals(202, delete(1, id).getStatus());
            createAndStart(0);
            
            assertEquals(200, repair(0).getStatus());
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var idElement :
                    ids) {
                checkResponse(200, value, get(0, idElement, 1, getClusterSize()));
            }
            
            assertEquals(404, get(0, id, 1, getClusterSize()).getStatus());
        });
    }
    
    @Test
    void missedRangeDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 512;
            final var value = randomValue();
            final List<String> ids = new ArrayList<>(records * 2);
            final List<String> missedIds = new ArrayList<>(records);
            long startMissedRange = Long.MAX_VALUE;
            long endMissedRange = 0;
            
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
    
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                missedIds.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            stop(0);
            for (int i = 0; i < records; i++) {
                final var id = missedIds.get(i);
                final var longHash =
                        longHash(hash(Record.of(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)),
                                ByteBuffer.wrap(value))));
                if (longHash < startMissedRange) {
                    startMissedRange = longHash;
                }
                if (longHash > endMissedRange) {
                    endMissedRange = longHash;
                }
                assertEquals(202, delete(1, id).getStatus());
            }
            
            createAndStart(0);
            for (int i = 0; i < records; i++) {
                final var id = randomId();
                ids.add(id);
                assertEquals(201, upsert(1, id, value).getStatus());
            }
            
            assertEquals(200, repair(0, startMissedRange, endMissedRange).getStatus());
            
            for (final var idElement :
                    ids) {
                checkResponse(200, value, get(0, idElement, 1, getClusterSize()));
            }
            
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            for (final var idElement :
                    missedIds) {
                assertEquals(404, get(0, idElement, 1, getClusterSize()).getStatus());
            }
        });
    }
    
    @Test
    void missedValueRange() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            // Reference key
            final int records = 512;
            final var value = randomValue();
            
            stop(0);
            final var id = randomId();
            final var longHash =
                    longHash(hash(Record.of(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)),
                            ByteBuffer.wrap(value))));
            assertEquals(201, upsert(1, id, value).getStatus());
            createAndStart(0);
            
            assertEquals(200, repair(0, longHash, longHash).getStatus());
            
            for (int node = 1; node < getClusterSize(); node++) {
                stop(node);
            }
            
            assertEquals(404, get(0, id, 1, getClusterSize()).getStatus());
        });
    }
}
