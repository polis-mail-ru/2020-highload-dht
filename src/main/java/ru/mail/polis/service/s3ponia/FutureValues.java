package ru.mail.polis.service.s3ponia;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureValues<T> {
    final Collection<FutureValue<T>> futureValues;

    public FutureValues(Collection<FutureValue<T>> futureValues) {
        this.futureValues = futureValues;
    }

    public void add(FutureValue<T> futureValue) {
        futureValues.add(futureValue);
    }

    public CompletableFuture<Collection<T>> atLeast(final int min) {
        final var futureValues = this.futureValues;
        final var successCounter = new AtomicInteger(min);
        final var failuresCounter = new AtomicInteger(futureValues.size() - min + 1);
        final var futureCollection = new CopyOnWriteArrayList<T>();
        final var future = new CompletableFuture<Collection<T>>();
        futureValues.forEach((f) -> f.whenCompleteAsync((r, t) -> {
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
