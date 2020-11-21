package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ResolvedDeleteResponses implements ResolvedFutureReplicaResponses {
    private static final Logger logger = LoggerFactory.getLogger(ResolvedPutResponses.class);
    final CompletableFuture<Collection<Void>> futureValues;

    public ResolvedDeleteResponses(CompletableFuture<Collection<Void>> futureValues) {
        this.futureValues = futureValues;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return futureValues
                .thenApply(c -> new Response(Response.ACCEPTED, Response.EMPTY))
                .exceptionally(t -> {
                    logger.error("Exception in resolving PUT", t);
                    return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
                });
    }
}
