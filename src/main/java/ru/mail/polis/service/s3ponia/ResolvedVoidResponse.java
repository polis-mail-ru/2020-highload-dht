package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ResolvedVoidResponse implements ResolvedFutureReplicaResponse {
    private static final Logger logger = LoggerFactory.getLogger(ResolvedVoidResponse.class);
    final CompletableFuture<Collection<Void>> futureValues;

    public ResolvedVoidResponse(@NotNull final CompletableFuture<Collection<Void>> futureValues) {
        this.futureValues = futureValues;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return futureValues
                .thenApply(c -> Response.ok(Response.EMPTY))
                .exceptionally(t -> {
                    logger.error("Exception in resolving", t);
                    return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
                });
    }
}
