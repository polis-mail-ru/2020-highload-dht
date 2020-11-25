package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ResolvedVoidResponse implements ResolvedFutureReplicaResponse {
    private static final Logger logger = LoggerFactory.getLogger(ResolvedVoidResponse.class);
    final ResolvedFutureReplicaResponse resolvedResponse;

    public ResolvedVoidResponse(@NotNull final ResolvedFutureReplicaResponse resolvedResponse) {
        this.resolvedResponse = resolvedResponse;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return resolvedResponse.resolved()
                .exceptionally(t -> {
                    logger.error("Exception in resolving", t);
                    return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
                });
    }
}
