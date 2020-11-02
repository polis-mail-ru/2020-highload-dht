package ru.mail.polis.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class FuturesUtils {

    private FuturesUtils() {
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @NotNull
    public static <T> CompletableFuture<Collection<T>> atLeastAsync(
            @NotNull final Collection<CompletableFuture<T>> futures,
            final int successes) {
        final AtomicInteger successesLeft = new AtomicInteger(successes);
        final AtomicInteger errorsLeft = new AtomicInteger(futures.size() - successes + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();
        futures.forEach(f -> f.whenComplete((v, t) -> {
            if (t == null) {
                results.add(v);
                if (successesLeft.decrementAndGet() == 0) {
                    future.complete(results);
                }
            } else {
                if (errorsLeft.decrementAndGet() == 0) {
                    future.completeExceptionally(new IllegalStateException("Can't get " + successes + " values"));
                }
            }
        }));
        return future;
    }

}
