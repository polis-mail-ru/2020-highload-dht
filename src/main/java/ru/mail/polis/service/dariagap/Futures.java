package ru.mail.polis.service.dariagap;

import one.nio.http.Response;
import ru.mail.polis.util.NotEnoughReplicasException;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class Futures {
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_NOT_FOUND = 404;

    private Futures() {

    }

    private static boolean isCorrectResponse(final int responseStatus) {
        return responseStatus == STATUS_OK
                || responseStatus == STATUS_CREATED
                || responseStatus == STATUS_ACCEPTED
                || responseStatus == STATUS_NOT_FOUND;
    }

    /**
     * Returns collection of ack responses or NotEnoughReplicasException.
     */
    public static CompletableFuture<Collection<Response>> getAckResponses(
            final int ack,
            final Collection<CompletableFuture<Response>> futures) {
        final CompletableFuture<Collection<Response>> results = new CompletableFuture<>();
        final AtomicInteger ackCounter = new AtomicInteger(ack);
        final AtomicInteger errorCounter = new AtomicInteger(futures.size() - ack + 1);
        final Collection<Response> result = new CopyOnWriteArrayList<>();

        futures.forEach(f -> f.whenComplete((r,t) -> {
            if (t == null && isCorrectResponse(r.getStatus())) {
                result.add(r);
                if (ackCounter.decrementAndGet() == 0) {
                    results.complete(result);
                }
            } else {
                if (errorCounter.decrementAndGet() == 0) {
                    results.completeExceptionally(
                            new NotEnoughReplicasException("Not Enough Replicas"));
                }
            }
        }).isCancelled());
        return results;
    }
}
