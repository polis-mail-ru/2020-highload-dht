package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ResolvedPutResponse implements ResolvedFutureReplicaResponse {
    final ResolvedFutureReplicaResponse resolvedResponse;

    public ResolvedPutResponse(@NotNull final ResolvedFutureReplicaResponse resolvedResponse) {
        this.resolvedResponse = resolvedResponse;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return resolvedResponse.resolved()
                .thenApply(c -> new Response(Response.CREATED, Response.EMPTY));
    }
}
