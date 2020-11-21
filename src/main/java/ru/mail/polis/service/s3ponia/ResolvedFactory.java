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

    public ResolvedFactory(final int minResponses, final int requestMethod) {
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

    /**
     * Creates {@link ResolvedFutureReplicaResponse} corresponding to request method.
     * @param futureValues future values for resolving
     * @param <T> future's result type
     * @return a {@link ResolvedFutureReplicaResponse}
     * @throws InvalidRequestMethod throws on unsupported request method and mismatch of method and futureValues type
     */
    public <T> ResolvedFutureReplicaResponse resolvedFutureReplicaResponses(
            @NotNull final FutureValues<T> futureValues) throws InvalidRequestMethod {
        final var futureCollection = futureValues.atLeast(minResponses);
        try {
            switch (requestMethod) {
                case Request.METHOD_GET:
                    return new ResolvedGetResponse(castToGetFutureCollection(futureCollection));
                case Request.METHOD_DELETE:
                    return new ResolvedDeleteResponse(
                            new ResolvedVoidResponse(castToVoidFutureCollection(futureCollection))
                    );
                case Request.METHOD_PUT:
                    return new ResolvedPutResponse(
                            new ResolvedVoidResponse(castToVoidFutureCollection(futureCollection))
                    );
                default:
                    throw new InvalidRequestMethod("Unsupported request method");
            }
        } catch (ClassCastException e) {
            throw new InvalidRequestMethod("Mismatch request method and future values type", e);
        }
    }
}
