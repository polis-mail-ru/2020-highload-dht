package ru.mail.polis.s3ponia;

import com.google.common.base.Splitter;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.s3ponia.Table;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Utility {
    public static final String DEADFLAG_TIMESTAMP_HEADER = "XDeadFlagTimestamp";
    public static final String PROXY_HEADER = "X-Proxy-From";
    public static final String TIME_HEADER = "XTime";
    
    private Utility() {
    }
    
    public static class Header {
        public final String key;
        public final String value;
        
        private Header(@NotNull final String key, @NotNull final String value) {
            this.key = key;
            this.value = value;
        }
        
        private static Header getHeader(@NotNull final String key,
                                        @NotNull final String[] headers,
                                        final int headerCount) {
            final int keyLength = key.length();
            for (int i = 1; i < headerCount; ++i) {
                if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                    final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                    return new Header(headers[i], value);
                }
            }
            
            return null;
        }
        
        /**
         * Get header with key from request.
         *
         * @param key     header's key
         * @param request request with headers
         * @return request's header
         */
        public static Header getHeader(@NotNull final String key, @NotNull final Request request) {
            final var headers = request.getHeaders();
            final var headerCount = request.getHeaderCount();
            
            return getHeader(key, headers, headerCount);
        }
        
        /**
         * Get header with key from response.
         *
         * @param key      header's key
         * @param response response with headers
         * @return response's header
         */
        public static Header getHeader(@NotNull final String key, @NotNull final Response response) {
            final var headers = response.getHeaders();
            final var headerCount = response.getHeaderCount();
            
            return getHeader(key, headers, headerCount);
        }
    }
    
    public static class ReplicationConfiguration {
        public final int ack;
        public final int from;
        
        public ReplicationConfiguration(final int ack, final int from) {
            this.ack = ack;
            this.from = from;
        }
        
        /**
         * Parses ReplicationConfiguration from String.
         *
         * @param s String for parsing
         * @return ReplicationConfiguration on null
         */
        public static ReplicationConfiguration parse(@NotNull final String s) {
            final var splitStrings = Splitter.on('/').splitToList(s);
            if (splitStrings.size() != 2) {
                return null;
            }
            
            try {
                return new ReplicationConfiguration(Integer.parseInt(splitStrings.get(0)),
                        Integer.parseInt(splitStrings.get(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
    
    public static boolean validateId(@NotNull final String id) {
        return id.isEmpty();
    }
    
    public static ByteBuffer byteBufferFromString(@NotNull final String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Checks is homeNode in nodeReplicas.
     * @param homeNode homeNode
     * @param nodeReplicas array of replicas
     * @return is home in replicas
     */
    public static boolean isHomeInReplicas(@NotNull final String homeNode,
                                           @NotNull final String... nodeReplicas) {
        boolean homeInReplicas = false;
        
        for (final var node :
                nodeReplicas) {
            if (node.equals(homeNode)) {
                homeInReplicas = true;
                break;
            }
        }
        return homeInReplicas;
    }
    
    /**
     * Getting DeadFlagTimeStamp from Table.Value.
     * @param response response
     * @return DeadFlagTimeStamp
     */
    public static long getDeadFlagTimeStamp(@NotNull final Response response) {
        final var header = Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        assert header != null;
        return Long.parseLong(header.value);
    }
    
    /**
     * Get Future values and store to list.
     * @param configuration replication configuration
     * @param futureResponses list of future responses
     * @return list of Table.Value
     */
    @NotNull
    public static List<Table.Value> getValuesFromFutures(@NotNull final ReplicationConfiguration configuration,
                                                         @NotNull final List<Future<Response>> futureResponses) {
        final List<Table.Value> values = new ArrayList<>(configuration.from);
        for (final var resp :
                futureResponses) {
            final Response response;
            try {
                response = resp.get();
            } catch (InterruptedException | ExecutionException e) {
                continue;
            }
            if (response != null && response.getStatus() == 200 /* OK */) {
                final var val = Table.Value.of(ByteBuffer.wrap(response.getBody()),
                        getDeadFlagTimeStamp(response), -1);
                values.add(val);
            }
        }
        return values;
    }
    
    /**
     * Response's value validation.
     * @return comparator
     */
    @NotNull
    public static Comparator<Table.Value> valueResponseComparator() {
        return Comparator.comparing(Table.Value::getTimeStamp)
                .reversed()
                .thenComparing((a, b) -> {
                    if (a.isDead()) {
                        return -1;
                    }
                    if (b.isDead()) {
                        return 1;
                    }
    
                    return 0;
                });
    }
    
    /**
     * Make map String to HttpClient from nodes list.
     * @param homeNode node ignored in nodes
     * @param nodes list of nodes
     * @return map string to httpclient
     */
    public static Map<String, HttpClient> urltoClientFromSet(@NotNull final String homeNode,
                                                             @NotNull final String... nodes) {
        final Map<String, HttpClient> result = new HashMap<>();
        for (final var url : nodes) {
            if (url.equals(homeNode)) {
                continue;
            }
            if (result.put(url, new HttpClient(new ConnectionString(url))) != null) {
                throw new RuntimeException("Duplicated url in nodes.");
            }
        }
        return result;
    }
    
    /**
     * Make byte array from ByteBuffer.
     * @param b ByteBuffer
     * @return byte array
     */
    @NotNull
    public static byte[] fromByteBuffer(@NotNull final ByteBuffer b) {
        final byte[] out = new byte[b.remaining()];
        b.get(out);
        return out;
    }
}
