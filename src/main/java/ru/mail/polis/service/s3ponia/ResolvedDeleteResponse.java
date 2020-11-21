package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ResolvedDeleteResponse implements ResolvedFutureReplicaResponse {
    final ResolvedFutureReplicaResponse resolvedResponse;

    public ResolvedDeleteResponse(@NotNull final ResolvedFutureReplicaResponse resolvedResponse) {
        this.resolvedResponse = resolvedResponse;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return resolvedResponse.resolved()
                .thenApply(c -> new Response(Response.ACCEPTED, Response.EMPTY));
    }
}
