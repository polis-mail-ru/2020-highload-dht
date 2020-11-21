package ru.mail.polis.service.s3ponia;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.s3ponia.Value;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResolvedFactory {
    private final int minResponses;
    private final int requestMethod;

    public ResolvedFactory(int minResponses, int requestMethod) {
        this.minResponses = minResponses;
        this.requestMethod = requestMethod;
    }

    private static <T> CompletableFuture<Collection<Value>> castToGetFutureCollection(
            @NotNull final CompletableFuture<Collection<T>> futureValues
    ) {
        return futureValues
                .thenApply(c -> c.stream()
                        .map(Value.class::cast)
                        .collect(Collectors.toList()));
    }

    private static <T> CompletableFuture<Collection<Void>> castToVoidFutureCollection(
            @NotNull final CompletableFuture<Collection<T>> futureValues
    ) {
        return futureValues
                .thenApply(c -> c.stream()
                        .map(Void.class::cast)
                        .collect(Collectors.toList()));
    }

    public <T> ResolvedFutureReplicaResponses resolvedFutureReplicaResponses(
            @NotNull final FutureValues<T> futureValues
    ) throws InvalidRequestMethod {
        final var futureCollection = futureValues.atLeast(minResponses);
        switch (requestMethod) {
            case Request.METHOD_GET:
                return new ResolvedGetResponses(castToGetFutureCollection(futureCollection));
            case Request.METHOD_DELETE:
                return new ResolvedDeleteResponses(castToVoidFutureCollection(futureCollection));
            case Request.METHOD_PUT:
                return new ResolvedPutResponses(castToVoidFutureCollection(futureCollection));
            default:
                throw new InvalidRequestMethod("Unhandled request method in resolver");
        }
    }
}
