package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

final class Futures {

    private Futures() {
    }

    @NotNull
    static <T> CompletableFuture<Collection<T>> atLeastAsync(
            final int successes,
            @NotNull final Collection<CompletableFuture<T>> futures) {
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();
        final AtomicInteger successesLeft = new AtomicInteger(successes);
        final AtomicInteger failuresLeft = new AtomicInteger(futures.size() - successes + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        futures.forEach(f -> f.whenComplete((v, t) -> {
            if (t == null) {
                results.add(v);
                if (successesLeft.decrementAndGet() == 0) {
                    future.complete(results);
                }
            } else {
                if (failuresLeft.decrementAndGet() == 0) {
                    future.completeExceptionally(t);
                }
            }
        }).isCancelled());

        return future;
    }
}
