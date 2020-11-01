package ru.mail.polis.service.mrsandman5.client;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.utils.ResponseUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static ru.mail.polis.utils.ResponseUtils.PROXY;

public class AsyncClient implements Client {

    private static final String ENTITY = "/v0/entity";
    private static final int TIMEOUT_MILLIS = 1000;

    private final String node;
    private final HttpClient client;
    private final Executor executor;

    public AsyncClient(@NotNull final String node,
                       @NotNull final Executor executor) {
        this.node = node;
        this.executor = executor;
        this.client = HttpClient.newBuilder()
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @NotNull
    @Override
    public CompletableFuture<Entry> getAsync(@NotNull final String id) {
        final HttpRequest request = requestForReplica(id)
                .GET()
                .build();
        return client.sendAsync(request, GetBodyHandler.INSTANCE)
                .thenApplyAsync(HttpResponse::body, executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> putAsync(@NotNull final String id,
                                            @NotNull final byte[] body) {
        final var data = HttpRequest.BodyPublishers.ofByteArray(body);
        final var request = requestForReplica(id)
                .PUT(data)
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApplyAsync(x -> null, executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> deleteAsync(@NotNull final String id) {
        final var request = requestForReplica(id)
                .DELETE()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApplyAsync(x -> null, executor);
    }

    @NotNull
    private HttpRequest.Builder requestForReplica(@NotNull final String id) {
        final String uri = node + ENTITY + "?id=" + id;
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header(PROXY, "True")
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS));
    }

    static final class GetBodyHandler implements HttpResponse.BodyHandler<Entry> {

        static final HttpResponse.BodyHandler<Entry> INSTANCE = new GetBodyHandler();

        private GetBodyHandler() {
        }

        @Override
        public HttpResponse.BodySubscriber<Entry> apply(
                @NotNull final HttpResponse.ResponseInfo responseInfo) {
            switch (responseInfo.statusCode()) {
                case 200:
                    final Optional<String> okTimestamp =
                            responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
                    if (okTimestamp.isEmpty()) {
                        throw new IllegalArgumentException("No timestamp header");
                    }
                    return HttpResponse.BodySubscribers.mapping(
                            HttpResponse.BodySubscribers.ofByteArray(),
                            bytes -> Entry.present(Long.parseLong(okTimestamp.get()), bytes));
                case 404:
                    final Optional<String> notFoundTimestamp =
                            responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
                    if (notFoundTimestamp.isEmpty()) {
                        return HttpResponse.BodySubscribers.replacing(
                                Entry.absent());
                    } else {
                        return HttpResponse.BodySubscribers.replacing(
                                Entry.removed(Long.parseLong(notFoundTimestamp.get())));
                    }
                default:
                    throw new RejectedExecutionException("Can't get response");
            }
        }
    }

}
