package ru.mail.polis.service.alexander.marashov.analyzers;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureAnalyzer {

    private FutureAnalyzer() {

    }

    public static <T> CompletableFuture<Collection<T>> atLeastAsync(
            final Collection<CompletableFuture<T>> futureCollection,
            final int neededCount
    ) {
        assert 0 < neededCount && neededCount <= futureCollection.size();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final AtomicInteger successesLeft = new AtomicInteger(neededCount);
        final AtomicInteger failuresLeft = new AtomicInteger(futureCollection.size() - neededCount + 1);
        futureCollection.forEach(f -> {
            final boolean canceled = f.whenComplete((response, error) -> {
                if (error == null) {
                    results.add(response);
                    if (successesLeft.decrementAndGet() <= 0) {
                        future.complete(results);
                    }
                } else if (failuresLeft.decrementAndGet() <= 0) {
                    future.completeExceptionally(error);
                }
            }).isCancelled();
            if (canceled) {
                throw new RuntimeException("Who canceled my future??");
            }
        });
        return future;
    }
}
