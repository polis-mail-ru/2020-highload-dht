package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;

import java.util.concurrent.CompletableFuture;

public interface ResolvedFutureReplicaResponse {
    CompletableFuture<Response> resolved();
}
