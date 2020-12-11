package ru.mail.polis.service.nik27090;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

final class Futures {
    private Futures() {
        // utility methods
    }

    @NotNull
    @SuppressWarnings("FutureReturnValueIgnored")
    static CompletableFuture<Collection<Response>> atLeastAsync(
            final int ack,
            final Collection<CompletableFuture<Response>> futures) {
        final AtomicInteger successesLeft = new AtomicInteger(ack);
        final AtomicInteger failuresLeft = new AtomicInteger(futures.size() - ack + 1);

        final Collection<Response> successResults = new CopyOnWriteArrayList<>();
        final Collection<Response> failResults = new CopyOnWriteArrayList<>();

        final CompletableFuture<Collection<Response>> future = new CompletableFuture<>();

        futures.forEach(f -> f.whenComplete((v, t) -> {
            if (t == null && v.getStatus() != 500) {
                successResults.add(v);
                if (successesLeft.decrementAndGet() == 0) {
                    future.complete(successResults);
                }
            } else {
                failResults.add(v);
                if (failuresLeft.decrementAndGet() == 0) {
                    future.complete(failResults);
                }
            }
        }));

        return future;
    }
}
