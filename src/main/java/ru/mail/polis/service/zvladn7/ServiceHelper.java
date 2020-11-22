package ru.mail.polis.service.zvladn7;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.zvladn7.Value;
import ru.mail.polis.dao.zvladn7.exceptions.DeletedValueException;
import ru.mail.polis.service.zvladn7.bodyhandlers.ChangeBodyHandler;
import ru.mail.polis.service.zvladn7.bodyhandlers.GetBodyHandler;
import ru.mail.polis.service.zvladn7.topology.Topology;
import ru.mail.polis.service.zvladn7.util.Nets;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static ru.mail.polis.service.zvladn7.util.Bytes.toBytes;
import static ru.mail.polis.service.zvladn7.util.Bytes.wrapArray;
import static ru.mail.polis.service.zvladn7.util.Bytes.wrapString;
import static ru.mail.polis.service.zvladn7.util.Nets.requestBuilderFor;

class ServiceHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceHelper.class);
    private static final String IO_EXCEPTION_ON_LOCAL_MESSAGE = "Can't execute local request";

    private final Topology<String> topology;

    @NotNull
    private final java.net.http.HttpClient client;
    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService es;

    /**
     * Helper for asynchronous server implementation.
     *
     * @param topology - topology of local node
     * @param dao      - DAO implemenation
     * @param es       - asynchronous service executor
     */
    ServiceHelper(@NotNull final Topology<String> topology,
                  @NotNull final DAO dao,
                  @NotNull final ExecutorService es) {
        this.topology = topology;
        this.dao = dao;
        this.es = es;
        final ExecutorService clientES = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("async-client-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error when processing request in: {}", t, e))
                        .build()
        );
        this.client = java.net.http.HttpClient.newBuilder()
                .executor(clientES)
                .connectTimeout(Duration.ofMillis(Nets.TIMEOUT))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
    }

    private CompletableFuture<ResponseValue> localGet(@NotNull final ByteBuffer key,
                                                      @NotNull final String id) {
        return CompletableFuture.supplyAsync(() -> {
            final Value value;
            try {
                value = dao.getValue(key);
                log.debug("Value successfully got!");
                return getLocalValue(value);
            } catch (IOException e) {
                log.error("Internal error. Can't get value with key: {}", id, e);
                throw new RuntimeException(IO_EXCEPTION_ON_LOCAL_MESSAGE, e);
            } catch (NoSuchElementException e) {
                log.info("Value with key: {} was not found", id, e);
                return ResponseValue.absent();
            }
        }, es);
    }

    CompletableFuture<Response> handleGet(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final ReplicasHolder replicasHolder) throws IOException {
        final ByteBuffer key = wrapString(id);
        return handleGetOrProxy(key, request, replicasHolder, () -> localGet(key, id), this::resolveGet);
    }

    private <T> List<CompletableFuture<T>> proxy(@NotNull final Set<String> nodesForResponse,
                                                 @NotNull final String method,
                                                 @NotNull final HttpResponse.BodyHandler<T> handler,
                                                 @NotNull final Function<String, HttpRequest> requestProvider) {
        log.debug("Proxy request: {} from {} to {}", method, topology.local(), nodesForResponse);
        final List<CompletableFuture<T>> responses = new ArrayList<>();
        nodesForResponse.forEach(node -> {
            final HttpRequest request = requestProvider.apply(node);
            final CompletableFuture<T> futureResponse =
                    client.sendAsync(request, handler)
                            .thenApply(HttpResponse::body);
            responses.add(futureResponse);
        });
        return responses;
    }

    private CompletableFuture<Response> handleGetOrProxy(@NotNull final ByteBuffer key,
                                                         @NotNull final Request request,
                                                         @NotNull final ReplicasHolder replicasHolder,
                                                         @NotNull final LocalExecutor<ResponseValue> localExecutor,
                                                         @NotNull final Resolver<ResponseValue> resolver
    ) throws IOException {
        final String header = request.getHeader(Nets.PROXY_REQUEST_HEADER);
        log.debug("Header: {}", header);
        final Set<String> nodesForResponse = topology.nodesForKey(key, replicasHolder.from);
        CompletableFuture<ResponseValue> localResponse = null;
        log.debug(nodesForResponse.toString());
        if (topology.isLocal(nodesForResponse)) {
            nodesForResponse.remove(topology.local());
            localResponse = localExecutor.execute();
            if (header != null) {
                return localResponse.thenApply(ResponseValue::toProxyResponse);
            }
        }
        List<CompletableFuture<ResponseValue>> responses;
        responses = proxy(nodesForResponse, request.getMethodName(), GetBodyHandler.INSTANCE,
                node -> requestBuilderFor(node, request.getParameter("id=")).GET().build());
        if (localResponse != null) {
            responses.add(localResponse);
        }
        return resolver.resolve(replicasHolder.ack, responses);
    }

    private CompletableFuture<Response> resolveGet(final int ack,
                                                   @NotNull final List<CompletableFuture<ResponseValue>> futures) {
        return ConflictResolver.atLeastAsync(futures, ack, es)
                .thenApply(collection -> ResponseValue.toResponse(ConflictResolver.resolveGet(collection)));
    }

    private CompletableFuture<String> localDelete(@NotNull final ByteBuffer key,
                                                  @NotNull final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.remove(key);
                log.debug("Value successfully deleted!");
            } catch (IOException e) {
                log.error("Internal error. Can't delete value with key: {}", id, e);
                throw new RuntimeException(IO_EXCEPTION_ON_LOCAL_MESSAGE, e);
            }
            return Response.ACCEPTED;
        }, es);
    }

    CompletableFuture<Response> handleDelete(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final ReplicasHolder replicasHolder) throws IOException {
        final ByteBuffer key = wrapString(id);
        return handleChangeOrProxy(key, request, replicasHolder, () -> localDelete(key, id), this::resolveChange);
    }

    private CompletableFuture<String> localUpsert(@NotNull final ByteBuffer key,
                                                  @NotNull final String id,
                                                  @NotNull final Request request) {
        return CompletableFuture.supplyAsync(() -> {
            final ByteBuffer value = wrapArray(request.getBody());
            try {
                dao.upsert(key, value);
                log.debug("Value successfully upserted!");
            } catch (IOException e) {
                log.error("Internal error. Can't insert or update value with key: {}", id, e);
                throw new RuntimeException(IO_EXCEPTION_ON_LOCAL_MESSAGE, e);
            }
            return Response.CREATED;
        }, es);
    }

    CompletableFuture<Response> handleUpsert(
            @NotNull final String id,
            @NotNull final Request request,
            @NotNull final ReplicasHolder replicasHolder) throws IOException {
        final ByteBuffer key = wrapString(id);
        return handleChangeOrProxy(
                key, request, replicasHolder, () -> localUpsert(key, id, request), this::resolveChange);
    }

    private CompletableFuture<Response> resolveChange(final int ack,
                                                      @NotNull final List<CompletableFuture<String>> futures) {
        return ConflictResolver.atLeastAsync(futures, ack, es)
                .thenApply(v -> new Response(v.iterator().next(), Response.EMPTY));
    }

    private CompletableFuture<Response> handleChangeOrProxy(final ByteBuffer key,
                                                            final Request request,
                                                            final ReplicasHolder replicasHolder,
                                                            final LocalExecutor<String> localExecutor,
                                                            final Resolver<String> resolver) throws IOException {
        final String header = request.getHeader(Nets.PROXY_REQUEST_HEADER);
        log.debug("Header: {}", header);
        final Set<String> nodesForResponse = topology.nodesForKey(key, replicasHolder.from);
        CompletableFuture<String> localResponse = null;
        log.debug(nodesForResponse.toString());
        if (topology.isLocal(nodesForResponse)) {
            nodesForResponse.remove(topology.local());
            localResponse = localExecutor.execute();
            if (header != null) {
                return localResponse.thenApply(v -> Nets.getChangeResponse(request.getMethodName()));
            }
        }
        List<CompletableFuture<String>> responses;
        responses = proxy(nodesForResponse,
                request.getMethodName(),
                ChangeBodyHandler.INSTANCE,
                node -> requestBuilderFor(node, request.getParameter("id="))
                        .method(request.getMethodName(), Nets.getBodyPublisher(request))
                        .build());
        if (localResponse != null) {
            responses.add(localResponse);
        }
        return resolver.resolve(replicasHolder.ack, responses);
    }

    private static ResponseValue getLocalValue(@NotNull final Value value) {
        try {
            final byte[] body = toBytes(value.getData());
            return ResponseValue.active(value.getTimestamp(), body);
        } catch (DeletedValueException ex) {
            return ResponseValue.deleted(value.getTimestamp());
        }
    }

}
