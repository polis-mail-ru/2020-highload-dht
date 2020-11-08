package ru.mail.polis.service.ivanovandrey;

import one.nio.http.Response;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class Futures {
    private Futures() {

    }

    public static CompletableFuture<Collection<Response>> getAckResponses(
            final int ack,
            final Collection<CompletableFuture<Response>> futures) {
        final CompletableFuture<Collection<Response>> results = new CompletableFuture<>();
        final AtomicInteger ackCounter = new AtomicInteger(ack);
        final AtomicInteger errorCounter = new AtomicInteger(futures.size() - ack + 1);
        final Collection<Response> result = new CopyOnWriteArrayList<>();
        futures.forEach(f -> f.whenComplete((r,t) -> {
            if (t == null) {
                result.add(r);
                if (ackCounter.decrementAndGet() == 0) {
                    results.complete(result);
                }
            } else {
                if (errorCounter.decrementAndGet() == 0) {
                    results.completeExceptionally(
                            new Exception("504 Not Enough Replicas"));
                }
            }
        }).isCancelled());
        return results;
    }
}