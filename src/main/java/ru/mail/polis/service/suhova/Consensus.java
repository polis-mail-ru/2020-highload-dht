package ru.mail.polis.service.suhova;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static ru.mail.polis.service.suhova.DAOServiceMethods.TOMBSTONE;
import static ru.mail.polis.service.suhova.DAOServiceMethods.VERSION;

public final class Consensus {
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private static final Logger logger = LoggerFactory.getLogger(Consensus.class);

    private Consensus() {
    }

    /**
     * Resolves conflicts among responses received from a GET request.
     *
     * @param responses - list of all responses
     * @param acks      - acks
     * @return resulting response
     */
    public static Response get(final Collection<CompletableFuture<Response>> responses, final int acks) {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger count404 = new AtomicInteger(0);
        AtomicReference<Response> okValue = new AtomicReference<>(new Response(Response.NOT_FOUND, Response.EMPTY));
        AtomicLong finalLastVersion = new AtomicLong(0);
        for (final CompletableFuture<Response> future : responses) {
            if (future.whenCompleteAsync((response, t) -> {
                final int status = response.getStatus();
                if (status == 200) {
                    count.getAndDecrement();
                    final AtomicLong version = new AtomicLong(Long.parseLong(response.getHeader(VERSION)));
                    if (version.get() > finalLastVersion.get()) {
                        finalLastVersion.getAndSet(version.get());
                        okValue.set(response);
                    }
                } else if (status == 404) {
                    count404.getAndDecrement();
                    count.getAndDecrement();
                }
            }).isCancelled()) {
                logger.error("Cancelled in GET");
            }
        }
        logger.debug("Success: {}, Ack: {}", count.get(), acks);
        if (count.get() >= acks) {
            if (Boolean.parseBoolean(okValue.get().getHeader(TOMBSTONE)) || count.get() == count404.get()) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } else {
                return Response.ok(okValue.get().getBody());
            }
        } else {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
    }

    /**
     * Resolves conflicts among responses received from a PUT request.
     *
     * @param responses - list of all responses
     * @param acks      - ack
     * @return resulting response
     */
    public static Response put(final Collection<CompletableFuture<Response>> responses, final int acks) {
        return successIfEnoughAcks(responses, acks, 201, Response.CREATED);
    }

    /**
     * Resolves conflicts among responses received from a DELETE request.
     *
     * @param responses - list of all responses
     * @param acks      - ack
     * @return resulting response
     */
    public static Response delete(final Collection<CompletableFuture<Response>> responses, final int acks) {
        return successIfEnoughAcks(responses, acks, 202, Response.ACCEPTED);
    }

    private static Response successIfEnoughAcks(final Collection<CompletableFuture<Response>> responses,
                                                final int acks,
                                                final int status,
                                                final String result) {
        AtomicInteger ackCount = new AtomicInteger(0);
        for (final CompletableFuture<Response> future : responses) {
            if (future.whenCompleteAsync((response, t) -> {
                    if (response.getStatus() == status) {
                        ackCount.getAndDecrement();
                    }
                }
            ).isCancelled()) {
                logger.error("Cancelled!");
            }
        }
        if (ackCount.get() >= acks) {
            return new Response(result, Response.EMPTY);
        } else {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
    }
}
