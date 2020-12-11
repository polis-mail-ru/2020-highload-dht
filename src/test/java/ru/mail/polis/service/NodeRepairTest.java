package ru.mail.polis.service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class NodeRepairTest extends NodeRepairTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    
    @Test
    void singleRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            for (int node = 0; node < getClusterSize(); node++) {
                final String key = "key" + node;
                final byte[] value = randomValue();
                
                try (final var ignored = new StoppedNode(node)) {
                    upsertExceptOne(ignored.node, key, value);
                }
                
                assertEquals(200, repair(node).getStatus());
                
                stopExceptOne(node);
                
                checkResponse(200, value, get(node, key, 1, 3));
                
                stop(node);
                
                runAll();
            }
        });
    }
    
    @Test
    void allRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int records = 1024;
            final Collection<RecordRequest> ids = new ArrayList<>(getClusterSize() * records);
            
            try (final var ignored = new StoppedNode(0)) {
                ids.addAll(fillRandomValues(ignored.node + 1, records));
            }
            
            assertEquals(200, repair(0).getStatus());
            
            stopExceptOne(0);
            
            for (final var id : ids) {
                checkResponse(200, id.value, get(0, id.id, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void noRepair() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int records = 12_000;
            
            final var ids = fillRandomValues(1, records);
            
            assertEquals(200, repair(0).getStatus());
            
            stopExceptOne(0);
            
            for (final var id :
                    ids) {
                checkResponse(200, id.value, get(0, id.id, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void missedValue() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int records = 512;
            
            final var ids = fillRandomValues(1, records);
            
            try (final var ignored = new StoppedNode(0)) {
                final var recordReq = fillRandomValue(ignored.node + 1);
                ids.add(recordReq);
                assertEquals(201, upsert(1, recordReq.id, recordReq.value).getStatus());
            }
            
            assertEquals(200, repair(0).getStatus());
            
            stopExceptOne(0);
            
            for (final var idElement : ids) {
                checkResponse(200, idElement.value, get(0, idElement.id, 1, getClusterSize()));
            }
        });
    }
    
    @Test
    void missedRange() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int records = 512;
            long startMissedRange = Long.MAX_VALUE;
            long endMissedRange = 0;
            
            final Collection<RecordRequest> ids = fillRandomValues(1, records);
            final Collection<RecordRequest> missedRecs;
            
            try (final var ignored = new StoppedNode(0)) {
                missedRecs = fillRandomValues(ignored.node + 1, records);
                
                for (final var rec :
                        missedRecs) {
                    final var longHash = longHash(hash(rec.toRecord()));
                    startMissedRange = Math.min(startMissedRange, longHash);
                    endMissedRange = Math.max(endMissedRange, longHash);
                }
            }
            
            ids.addAll(fillRandomValues(1, records));
            
            assertEquals(200, repair(0, startMissedRange, endMissedRange + 1).getStatus());
            
            checkGetExist(0, ids);
            
            stopExceptOne(0);
            
            checkGetExist(0, missedRecs);
        });
    }
    
    @Test
    void missedDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int records = 512;
            final var value = randomValue();
            
            final var ids = fillRandomValues(1, records);
            
            final var id = randomId();
            assertEquals(201, upsert(1, id, value).getStatus());
            
            try (final var ignored = new StoppedNode(0)) {
                assertEquals(202, delete(ignored.node + 1, id).getStatus());
            }
            
            assertEquals(200, repair(0).getStatus());
            
            stopExceptOne(0);
            
            checkGetExist(0, ids);
            
            assertEquals(404, get(0, id, 1, getClusterSize()).getStatus());
        });
    }
    
    @Test
    void missedRangeDelete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final int recordsCount = 512;
            long startMissedRange = Long.MAX_VALUE;
            long endMissedRange = 0;

            final var records = fillRandomValues(1, recordsCount);
            final var missedRecords = fillRandomValues(1, recordsCount);

            try (final var ignored = new StoppedNode(0)) {
                deleteRange(ignored.node + 1, missedRecords);
                for (final var rec : missedRecords) {
                    final var longHash =
                            longHash(deadHash(rec.toRecord()));
                    startMissedRange = Math.min(startMissedRange, longHash);
                    endMissedRange = Math.max(endMissedRange, longHash);
                }
            }

            records.addAll(fillRandomValues(1, recordsCount));

            assertEquals(200, repair(0, startMissedRange, endMissedRange + 1).getStatus());

            checkGetExist(0, records);

            stopExceptOne(0);

            checkGetDeleted(0, missedRecords);
        });
    }
    
    @Test
    void missedValueRange() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final long longHash;
            final RecordRequest rec;
    
            try (final var ignored = new StoppedNode(0)) {
                rec = fillRandomValue(ignored.node + 1);
                longHash = longHash(hash(rec.toRecord()));
            }
            
            assertEquals(200, repair(0, longHash, longHash + 1).getStatus());
            
            stopExceptOne(0);
            
            checkResponse(200, rec.value, get(0, rec.id, 1, getClusterSize()));
        });
    }
    
    @Test
    void rewriteValue() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            final long longHash;
            final RecordRequest record;
            
            try (final var ignored = new StoppedNode(0)) {
                record = fillRandomValue(ignored.node + 1);
                longHash =
                        longHash(hash(record.toRecord()));
            }
            
            final var value2 = randomValue();
            assertEquals(201, upsert(1, record.id, value2).getStatus());
            
            assertEquals(200, repair(0, longHash, longHash).getStatus());
            
            stopExceptOne(0);
            
            checkResponse(200, value2, get(0, record.id, 1, getClusterSize()));
        });
    }
    
    @Test
    void resurrectValue() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            
            final var record = fillRandomValue(1);
            final var longHash = longHash(hash(record.toRecord()));
    
            try (final var ignored = new StoppedNode(0)) {
                assertEquals(202, delete(ignored.node + 1, record.id).getStatus());
            }
            
            final var value2 = randomValue();
            assertEquals(201, upsert(1, record.id, value2).getStatus());
            
            assertEquals(200, repair(0, longHash, longHash).getStatus());
            
            stopExceptOne(0);
            
            checkResponse(200, value2, get(0, record.id, 1, getClusterSize()));
        });
    }
}
