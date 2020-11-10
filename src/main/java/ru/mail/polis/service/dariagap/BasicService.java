package ru.mail.polis.service.dariagap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class BasicService {
    private final DAO dao;
    private final HttpClient client;
    private final ExecutorService execPool;

    private static final Logger log = LoggerFactory.getLogger(BasicService.class);

    private static final String PROXY_HEADER = "X-OK-Proxy";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private static final String INTERNAL_ERROR = "Internal Server Error";
    private static final String NOT_FOUND_ERROR = "Data not found";

    /**
     * Config HttpClient, DAO and ExecutorService.
     *
     * @param dao - storage interface
     * @param executors - number of executors
     * @param execPool - ExecutorService
     */
    public BasicService(@NotNull final DAO dao,
                        @NotNull final int executors,
                        @NotNull final ExecutorService execPool) {
        this.dao = dao;
        final Executor exec = Executors.newFixedThreadPool(
                executors,
                new ThreadFactoryBuilder()
                        .setNameFormat("Client-%d")
                        .build());
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .executor(exec)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.execPool = execPool;
    }

    /**
     * Get value by id.
     */
    public CompletableFuture<Response> get(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final ByteBuffer value = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return new Response(Response.OK, Util.byteBufferToBytes(value));
            } catch (IOException ex) {
                log.error(INTERNAL_ERROR, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            } catch (NoSuchElementException ex) {
                log.error(NOT_FOUND_ERROR, ex);
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Insert or update value by id.
     */
    public CompletableFuture<Response> put(final String id,final byte[] body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.upsertWithTimestamp(
                        ByteBuffer.wrap(id.getBytes(UTF_8)),
                        ByteBuffer.wrap(body));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(INTERNAL_ERROR, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Delete value by id.
     */
    public CompletableFuture<Response> delete(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.removeWithTimestamp(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(INTERNAL_ERROR, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    private HttpRequest.Builder formHttpRequestBuilder(final String node, final String id)
            throws IllegalArgumentException {
        final URI uri = URI.create(node + "/v0/entity?id" + id);
        return HttpRequest.newBuilder()
                .header(PROXY_HEADER,"true")
                .timeout(TIMEOUT)
                .uri(uri);
    }

    private HttpRequest formHttpRequest(final String node, final Request request)
            throws NoSuchMethodException, IllegalArgumentException {
        final HttpRequest.Builder builder =
                formHttpRequestBuilder(node, request.getParameter("id"));
        switch (request.getMethod()) {
            case METHOD_GET:
                return builder.GET().build();
            case METHOD_PUT:
                return builder
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
            case METHOD_DELETE:
                return builder.DELETE().build();
            default:
                throw new NoSuchMethodException();
        }
    }

    private String getResponseStatus(final int statusCode) {
        switch (statusCode) {
            case 200:
                return Response.OK;
            case 201:
                return Response.CREATED;
            case 202:
                return Response.ACCEPTED;
            case 404:
                return Response.NOT_FOUND;
            default:
                return Response.INTERNAL_ERROR;
        }
    }

    /**
     * Proxy request to given node.
     */
    public CompletableFuture<Response> proxy(final String node, final Request request) {
        try {
            return client.sendAsync(formHttpRequest(node,request),
                    HttpResponse.BodyHandlers.ofByteArray())
                    .thenApplyAsync(httpResponse ->
                            new Response(
                                    getResponseStatus(httpResponse.statusCode()),
                                    httpResponse.body()),
                            execPool);
        } catch (NoSuchMethodException e) {
            log.error("Unknown method");
            return CompletableFuture.supplyAsync(() ->
                    new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
        }
    }

    public synchronized void stop() {
        execPool.shutdown();
        try {
            execPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Can not stop server.", ex);
            Thread.currentThread().interrupt();
        }
    }
}
