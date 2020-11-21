package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ResolvedPutResponse implements ResolvedFutureReplicaResponse {
    private static final Logger logger = LoggerFactory.getLogger(ResolvedPutResponse.class);
    final ResolvedFutureReplicaResponse resolvedResponse;

    public ResolvedPutResponse(ResolvedFutureReplicaResponse resolvedResponse) {
        this.resolvedResponse = resolvedResponse;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return resolvedResponse.resolved()
                .thenApply(c -> new Response(Response.CREATED, Response.EMPTY));
    }
}
