package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class FutureValue<T> {
    private static final Logger logger = LoggerFactory.getLogger(FutureValue.class);
    private final CompletableFuture<T> future;

    public FutureValue(@NotNull final CompletableFuture<T> future) {
        this.future = future
    ;
    }

    /**
     * Executes given action when future completes.
     * @param biConsumer the action to perform
     */
    public void whenCompleteAsync(@NotNull final BiConsumer<? super T, ? super Throwable> biConsumer) {
        if (future.whenCompleteAsync(biConsumer)
                .isCancelled()) {
            logger.error("Canceled future task");
            throw new FutureResponseException("Canceled future task");
        }
    }
}
