package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public class ResolvedGetResponses implements ResolvedFutureReplicaResponses {
    private static final Logger logger = LoggerFactory.getLogger(ResolvedGetResponses.class);
    final CompletableFuture<Collection<Value>> futureValues;

    public ResolvedGetResponses(CompletableFuture<Collection<Value>> futureValues) {
        this.futureValues = futureValues;
    }

    @Override
    public CompletableFuture<Response> resolved() {
        return futureValues
                .thenApply(c -> {
                            try {
                                return Collections.min(c, Value.valueResponseComparator());
                            } catch (NoSuchElementException e) {
                                return Value.ABSENT;
                            }
                        }
                )
                .thenApply(v -> {
                    if (v.isDead()) {
                        return new Response(Response.NOT_FOUND, Response.EMPTY);
                    }

                    return Response.ok(Utility.fromByteBuffer(v.getValue()));
                })
                .exceptionally(t -> {
                    logger.error("Exception in resolving GET", t);
                    return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
                });
    }
}
