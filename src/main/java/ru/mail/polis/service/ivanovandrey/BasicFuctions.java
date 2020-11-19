package ru.mail.polis.service.ivanovandrey;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

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

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class BasicFuctions {
    private static final String ERROR_MESSAGE = "Can't send response. Session {}";
    private static final String RESPONSE_ERROR = "Can not send response.";
    private static final String PROXY_HEADER = "X-OK-Proxy";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final Logger log = LoggerFactory.getLogger(RandezvouzTopology.class);
    private final HttpClient client;
    private final ExecutorService execPool;
    private final DAO dao;

    /**
     * Constructor.
     * @param dao       - dao.
     * @param execPool  - ExecutorService.
     */
    public BasicFuctions(@NotNull final DAO dao,
                         @NotNull final ExecutorService execPool,
                         final int executors) {
        this.dao = dao;
        this.execPool = execPool;
        final Executor executor = Executors.newFixedThreadPool(
                executors,
                new ThreadFactoryBuilder()
                        .setNameFormat("Client-%d")
                        .build());
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Get data by key.
     * @param id - key.
     */
    public CompletableFuture<Response> get(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final ByteBuffer val = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return Response.ok(Util.fromByteBufferToByteArray(val));
            } catch (NoSuchElementException e) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } catch (IOException e) {
                log.error("Error in get request", e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Put data by key.
     * @param id      - key.
     * @param request - request.
     */
    public CompletableFuture<Response> put(final String id, final byte[] request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.upsertWithTimestamp(
                        ByteBuffer.wrap(id.getBytes(UTF_8)),
                        ByteBuffer.wrap(request));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(ERROR_MESSAGE, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Delete data by key.
     * @param id - key.
     */
    public CompletableFuture<Response> delete(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.removeWithTimestamp(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(ERROR_MESSAGE, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Send response when future completes.
     * @param session - session.
     * @param response - response.
     */
    public void trySendResponse(final HttpSession session,
                                final CompletableFuture<Response> response) {
        if (response.whenComplete((r, t) -> {
            if (t == null) {
                try {
                    session.sendResponse(r);
                } catch (IOException ex) {
                    log.error(RESPONSE_ERROR, ex);
                }
            } else {
                try {
                    session.sendError(ERROR_MESSAGE, t.getMessage());
                } catch (IOException ex) {
                    log.error("Can not send error.", ex);
                }
            }
        }).isCancelled()) {
            log.error(RESPONSE_ERROR);
        }
    }

    private HttpRequest formHttpRequest(final String node, final Request request)
            throws NoSuchMethodException, IllegalArgumentException {
        final URI uri = URI.create(node + "/v0/entity?id" + request.getParameter("id"));
        final HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .header(PROXY_HEADER, "true")
                        .timeout(TIMEOUT)
                        .uri(uri);
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

    /**
     * Forward request to another node.
     */
    public CompletableFuture<Response> forwardRequest(final String node, final Request request) {
        try {
            return client.sendAsync(formHttpRequest(node,request),
                    HttpResponse.BodyHandlers.ofByteArray())
                    .thenApplyAsync(httpResponse ->
                                    new Response(
                                            String.valueOf(httpResponse.statusCode()),
                                            httpResponse.body()),
                            execPool);
        } catch (NoSuchMethodException e) {
            log.error("Unknown method");
            return CompletableFuture.supplyAsync(() ->
                    new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
        }
    }
}
