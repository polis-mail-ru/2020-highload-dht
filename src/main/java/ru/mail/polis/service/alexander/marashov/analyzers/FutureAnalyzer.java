package ru.mail.polis.service.alexander.marashov.analyzers;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class FutureAnalyzer {

    private FutureAnalyzer() {

    }

    /**
     * Transforms collection of futures to future-to-collection object.
     * @param futureCollection - collection of the future to T object.
     * @param neededCount - how many replicas should answer.
     * @param <T> - type of the collection elements.
     * @return future to collection object.
     */
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
                    future.completeExceptionally(
                            new IllegalStateException("Can't get " + neededCount + " answers", error)
                    );
                }
            }).isCancelled();
            if (canceled) {
                throw new IllegalStateException("Who canceled my future??");
            }
        });
        return future;
    }
}
