package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureValues<T> {
    final Collection<FutureValue<T>> collectionOfFutures;

    public FutureValues(@NotNull final Collection<FutureValue<T>> collectionOfFutures) {
        this.collectionOfFutures = collectionOfFutures;
    }

    public void add(@NotNull final FutureValue<T> futureValue) {
        collectionOfFutures.add(futureValue);
    }

    /**
     * Convert collection of futures to future of collection.
     * @param min minimum successfully completed {@code FutureValue<T>}'s for success
     * @return a {@code CompletableFuture<Collection<T>}
     */
    public CompletableFuture<Collection<T>> atLeast(final int min) {
        final var successCounter = new AtomicInteger(min);
        final var failuresCounter = new AtomicInteger(collectionOfFutures.size() - min + 1);
        final var futureCollection = new CopyOnWriteArrayList<T>();
        final var future = new CompletableFuture<Collection<T>>();
        collectionOfFutures.forEach((f) -> f.whenCompleteAsync((r, t) -> {
            if (t != null) {
                if (failuresCounter.decrementAndGet() == 0) {
                    future.completeExceptionally(new IllegalStateException("Too many failures"));
                }
                return;
            }
            futureCollection.add(r);

            if (successCounter.decrementAndGet() == 0) {
                future.complete(futureCollection);
            }
        }));

        return future;
    }
}
