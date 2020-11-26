package ru.mail.polis.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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
            final int records = 512;
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
}
