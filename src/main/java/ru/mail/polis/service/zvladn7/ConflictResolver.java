package ru.mail.polis.service.zvladn7;

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
        long theMostFreshedTimestamp = theMostFreshedResponse.getTimestamp();
        while (iterator.hasNext()) {
            final ResponseValue next = iterator.next();
            final long responseTimestamp = next.getTimestamp();
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
        final AtomicInteger failures = new AtomicInteger(futures.size() - ack + 1);
        futures.forEach(nextFuture -> {
            if (nextFuture.whenCompleteAsync((v, t) -> {
                if (t == null) {
                    results.add(v);
                    if (successes.decrementAndGet() == 0) {
                        resultFuture.complete(results);
                    }
                } else {
                    if (failures.decrementAndGet() == 0) {
                        resultFuture.completeExceptionally(new IllegalStateException("Not enough replicas to respond"));
                    }
                }
            }).isCancelled()) {
                log.error("Cannot resolve success or failure");
            }
        });

        return resultFuture;
    }

}
