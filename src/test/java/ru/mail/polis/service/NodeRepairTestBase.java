package ru.mail.polis.service;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.hash.ConcatHash;
import ru.mail.polis.util.hash.TigerHash;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.mail.polis.util.Utility.fromByteArray;

public class NodeRepairTestBase extends ClusterTestBase {
    
    @Override
    int getClusterSize() {
        return 3;
    }
    
    void stopAll() {
        for (int i = 0; i < getClusterSize(); i++) {
            stop(i);
        }
    }
    
    void runAll() throws Exception {
        for (int i = 0; i < getClusterSize(); i++) {
            createAndStart(i);
        }
    }
    
    void stopExceptOne(final int exceptOne) throws Exception {
        stopAll();
        createAndStart(exceptOne);
    }
    
    void upsertExceptOne(final int exceptedNode, final String key, final byte[] value) throws Exception {
        for (int i = (exceptedNode + 1) % getClusterSize(); i != exceptedNode; i = (i + 1) % getClusterSize()) {
            assertEquals(201, upsert(i, key, value).getStatus());
        }
    }
    
    class StoppedNode implements Closeable {
        private final Logger logger = LoggerFactory.getLogger(StoppedNode.class);
        final int node;
    
        StoppedNode(int node) {
            this.node = node;
            stop(node);
        }
    
        @Override
        public void close() throws IOException {
            try {
                createAndStart(node);
            } catch (Exception e) {
                logger.error("Error in creating and restarting node {}", node, e);
            }
        }
    }
    
    static class RecordRequest {
        final String id;
        final byte[] value;
    
        public RecordRequest(String id, byte[] value) {
            this.id = id;
            this.value = value;
        }
        
        public Record toRecord() {
            return Record.of(
                    ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)),
                    ByteBuffer.wrap(value)
            );
        }
        
        public static RecordRequest random() {
            return new RecordRequest(randomId(), randomValue());
        }
    }
    
    RecordRequest fillRandomValue(final int node) throws Exception {
        final var rec = RecordRequest.random();
        assertEquals(201, upsert(node, rec.id, rec.value, 2, 3).getStatus());
        return rec;
    }
    
    void checkGetExist(final int node, final Collection<RecordRequest> recordRequests) throws Exception {
        for (final var rec :
                recordRequests) {
            checkResponse(200, rec.value, get(node, rec.id, 1, getClusterSize()));
        }
    }
    
    void deleteRange(final int node, final Collection<RecordRequest> recordRequests) throws Exception {
        for (final var rec :
                recordRequests) {
            assertEquals(202, delete(node, rec.id).getStatus());
        }
    }
    
    void checkGetDeleted(final int node, final Collection<RecordRequest> recordRequests) throws Exception {
        for (final var rec :
                recordRequests) {
            assertEquals(404, get(0, rec.id, 1, getClusterSize()).getStatus());
        }
    }
    
    List<RecordRequest> fillRandomValues(final int node, final int count) throws Exception {
        final List<RecordRequest> upsertedRecords = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final var rec = RecordRequest.random();
            upsertedRecords.add(fillRandomValue(node));
        }
        
        return upsertedRecords;
    }

    private static final ConcatHash hash = new TigerHash();
    
    static byte[] hash(@NotNull final Record record) {
        final var byteBuffer =
                ByteBuffer
                        .allocate(
                                record.getKey().capacity()
                                        + record.getValue().capacity()
                                        + Long.BYTES /* IS_DEAD */
                        );
        byteBuffer.put(record.getKey());
        byteBuffer.put(record.getValue());
        byteBuffer.asLongBuffer().put(0L);
        return hash.hash(byteBuffer.array());
    }
    
    static byte[] deadHash(@NotNull final Record record) {
        final var byteBuffer =
                ByteBuffer
                        .allocate(
                                record.getKey().capacity()
                                        + Long.BYTES /* IS_DEAD */
                        );
        byteBuffer.put(record.getKey());
        byteBuffer.put(ByteBuffer.allocate(0));
        byteBuffer.asLongBuffer().put(Value.DEAD_FLAG);
        return hash.hash(byteBuffer.array());
    }
    
    static long longHash(@NotNull final byte[] hashArray) {
        return fromByteArray(hashArray, 0, Long.BYTES) & Long.MAX_VALUE;
    }
}
