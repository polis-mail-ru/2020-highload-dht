package ru.mail.polis.utils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public final class FuturesUtils {

    private static final Logger log = LoggerFactory.getLogger(FuturesUtils.class);

    private FuturesUtils() {
    }

    /** Create future of all requests' results.
     * @param futures - list of futures with requests' results.
     * @param successes - necessary number of successful requests
     * @param executor - thread pool of async executors
     * */
    @NotNull
    public static <T> CompletableFuture<Collection<T>> atLeastAsync(
            @NotNull final Collection<CompletableFuture<T>> futures,
            final int successes,
            @NotNull final ExecutorService executor) {
        final AtomicInteger successesLeft = new AtomicInteger(successes);
        final AtomicInteger errorsLeft = new AtomicInteger(futures.size() - successes + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> future = new CompletableFuture<>();
        futures.forEach(f -> f.whenCompleteAsync((v, t) -> {
            log.info("Inside future");
            if (t == null) {
                log.info("No exception found");
                results.add(v);
                if (successesLeft.decrementAndGet() == 0) {
                    future.complete(results);
                }
            } else {
                log.info("Exception found: ", t);
                if (errorsLeft.decrementAndGet() == 0) {
                    future.completeExceptionally(new IllegalStateException("Can't get " + successes + " values"));
                }
            }
        }, executor).isCancelled());
        return future;
    }

}
