package ru.mail.polis.service.ivanovandrey;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class SimpleTopology {
    private static final String PROXY_HEADER = "X-OK-Proxy";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final Logger log = LoggerFactory.getLogger(SimpleTopology.class);
    private final HttpClient client;
    private final ExecutorService execPool;
    private final String thisNode;

    public SimpleTopology(final int executors,
                          @NotNull final ExecutorService execPool,
                          int port) {
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
        this.execPool = execPool;
        this.thisNode = "http://localhost:" + port;
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

    public Boolean isCurrentNode(final String node) {
        return node.equals(this.thisNode);
    }
}
