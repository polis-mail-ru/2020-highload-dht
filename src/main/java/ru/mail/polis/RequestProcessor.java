package ru.mail.polis;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.TimestampRecord;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public abstract class RequestProcessor {
    private static final String PROXY_HEADER = "X-OK-Proxy: True";

    @FunctionalInterface
    public interface MyConsumer<T, U, R, Y, C> {
        void accept(T t, U u, R r, Y y, C c);
    }

    /**
     * Create list of requests.
     *
     * @param uris          - other nodes
     * @param rqst          - http request
     * @param methodDefiner - define http method of requests
     * @return list of HttpRequest
     */
    public static List<HttpRequest> createRequests(final List<String> uris,
                                                   final Request rqst,
                                                   final Function<HttpRequest.Builder,
                                                           HttpRequest.Builder> methodDefiner) {
        return uris.stream()
                .map(x -> x + rqst.getURI())
                .map(RequestProcessor::createURI)
                .map(HttpRequest::newBuilder)
                .map(x -> x.setHeader("X-OK-Proxy", "true"))
                .map(methodDefiner)
                .map(x -> x.timeout(Duration.of(5, SECONDS)))
                .map(HttpRequest.Builder::build)
                .collect(toList());
    }

    private static URI createURI(final String s) {
        try {
            return URI.create(s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ProcessRequestModel {
        public final ByteBuffer key;
        public final AtomicInteger recievidAcks = new AtomicInteger(0);
        public final Boolean proxied;
        public final Integer neededAcks;
        public final List<String> uris;
        public final List<TimestampRecord> responses = Collections.synchronizedList(new ArrayList<>());

        /**
         * Process request model.
         *
         * @param replicaNodes - replicas nodes
         * @param rqst - request
         * @param acks - count of ack
         */
        public ProcessRequestModel(final String[] replicaNodes,
                                   @NotNull final Request rqst,
                                   final int acks) {
            final String id = rqst.getParameter("id=");
            this.key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            this.proxied = rqst.getHeader(PROXY_HEADER) != null;
            this.uris = new ArrayList<>(Arrays.asList(replicaNodes));
            this.neededAcks = acks;
        }
    }
}
