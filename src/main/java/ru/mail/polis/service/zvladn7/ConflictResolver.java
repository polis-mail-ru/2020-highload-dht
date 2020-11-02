package ru.mail.polis.service.zvladn7;

import com.google.common.primitives.Longs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

final class ConflictResolver {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolver.class);

    private ConflictResolver() {
    }

    static ResponseValue resolveGet(@NotNull final Collection<ResponseValue> responses) {
        final Iterator<ResponseValue> iterator = responses.iterator();
        ResponseValue theMostFreshedResponse = iterator.next();
        long theMostFreshedTimestamp = theMostFreshedResponse.getTimpestamp();
        log.info("Response timestamp: {}", theMostFreshedTimestamp);
        while (iterator.hasNext()) {
            final ResponseValue next = iterator.next();
            final long responseTimestamp = next.getTimpestamp();
            log.info("Response timestamp: {}", responseTimestamp);
            if (responseTimestamp > theMostFreshedTimestamp) {
                theMostFreshedTimestamp = responseTimestamp;
                theMostFreshedResponse = next;
            }
        }

        return theMostFreshedResponse;
    }

    @NotNull
    static <T> CompletableFuture<Collection<T>> atLeastAsync(@NotNull final Collection<CompletableFuture<T>> futures,
                                                             final int ack) {
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> resultFuture = new CompletableFuture<>();
        final AtomicInteger successes = new AtomicInteger(ack);
        final AtomicInteger failures = new AtomicInteger(futures.size() - ack  + 1);
        futures.forEach(nextFuture -> nextFuture.whenCompleteAsync((v, t) -> {
            if (t == null) {
                results.add(v);
                log.info("Success");
                if (successes.decrementAndGet() == 0) {
                    resultFuture.complete(results);
                }
            } else {
                log.error("Exception");
                if (failures.decrementAndGet() == 0) {
                    resultFuture.completeExceptionally(new IllegalStateException("Not enough replicas to respond"));
                }
            }
        }));

        return resultFuture;
    }

    private static long getResponseTimestamp(@NotNull final ResponseValue value) {
        final byte[] body = value.getBody();
        if (body.length == 0) {
            return -1;
        }
        if (value.getState() == ResponseValue.State.ACTIVE) {
            final byte[] timestampBytes = new byte[Long.BYTES];
            System.arraycopy(body, body.length - Long.BYTES, timestampBytes, 0, Long.BYTES);
            return Longs.fromByteArray(timestampBytes);
        } else {
            return Longs.fromByteArray(body);
        }
    }
}
