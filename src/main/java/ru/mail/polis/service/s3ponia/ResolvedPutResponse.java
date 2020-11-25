package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ResolvedPutResponse implements ResolvedFutureReplicaResponse {
    final CompletableFuture<Collection<Void>> futureValues;

    public ResolvedPutResponse(@NotNull final CompletableFuture<Collection<Void>> futureValues) {
        this.futureValues = futureValues;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return futureValues
                .thenApply(c -> new Response(Response.CREATED, Response.EMPTY));
    }
}
