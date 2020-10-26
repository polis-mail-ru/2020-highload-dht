package ru.mail.polis.s3ponia;

import com.google.common.base.Splitter;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.s3ponia.AsyncService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    
    public static Response proxy(
            @NotNull final String node,
            @NotNull final Request request,
            @NotNull final AsyncService service) {
        try {
            request.addHeader(PROXY_HEADER + ":" + node);
            return service.getUrlToClient().get(node).invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            return null;
        }
    }
    
    @NotNull
    public static List<Future<Response>> getFutures(@NotNull final Request request,
                                                    @NotNull final Utility.ReplicationConfiguration parsed,
                                                    @NotNull final AsyncService service,
                                                    @NotNull final String... nodes) {
        final List<Future<Response>> futureResponses = new ArrayList<>(parsed.from);
        
        for (final var node :
                nodes) {
            
            if (!node.equals(service.getPolicy().homeNode())) {
                futureResponses.add(service.getEs().submit(() -> proxy(node, request, service)));
            }
        }
        return futureResponses;
    }
    
    public static int getCounter(@NotNull final Request request,
                                 @NotNull final ReplicationConfiguration parsed,
                                 @NotNull final AsyncService service,
                                 @NotNull final String... nodes) {
        final List<Future<Response>> futureResponses = getFutures(request, parsed, service, nodes);
        
        int acceptedCounter = 0;
        
        for (final var resp :
                futureResponses) {
            final Response response;
            try {
                response = resp.get();
            } catch (InterruptedException | ExecutionException e) {
                continue;
            }
            if (response != null
                        && (response.getStatus() == 202 /* ACCEPTED */ || response.getStatus() == 201 /* CREATED */)) {
                ++acceptedCounter;
            }
        }
        return acceptedCounter;
    }
}
