package ru.mail.polis.s3ponia;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static ru.mail.polis.s3ponia.Utility.sendResponse;

/**
 * Utility class for proxying requests to different nodes.
 */
public final class Proxy {
    public static final String PROXY_HEADER = "X-Proxy-From";

    private Proxy() {
    }

    private static Response proxy(
            @NotNull final Request request,
            @NotNull final HttpClient client) {
        try {
            request.addHeader(PROXY_HEADER + ":" + "proxied");
            return client.invoke(request);
        } catch (IOException | InterruptedException | HttpException | PoolException exception) {
            return null;
        }
    }

    /**
     * Get list of success responses from nodes.
     *
     * @param request      proxying request
     * @param toHttpClient function for mapping url to HttpClient
     * @param skipNode     node that should be skipped
     * @param nodes        array of nodes for proxy dest
     * @return list of sucess responses
     */
    @NotNull
    public static List<Response> proxyReplicas(@NotNull final Request request,
                                               @NotNull final Function<String, HttpClient> toHttpClient,
                                               @NotNull final String skipNode,
                                               @NotNull final String... nodes) {
        final List<Response> futureResponses = new ArrayList<>(nodes.length);

        for (final var node : nodes) {

            if (!node.equals(skipNode)) {
                final var response = proxy(request, toHttpClient.apply(node));
                if (response == null) {
                    continue;
                }
                if (response.getStatus() != 202 /* ACCEPTED */
                        && response.getStatus() != 201 /* CREATED */
                        && response.getStatus() != 200 /* OK */
                        && response.getStatus() != 404 /* NOT FOUND */) {
                    continue;
                }
                futureResponses.add(response);
            }
        }
        return futureResponses;
    }

    public static void proxyHandle(@NotNull final HttpSession session,
                                   @NotNull final Supplier<CompletableFuture<Response>> proxyHandler) {
        if (proxyHandler.get()
                .whenComplete((r, t) -> proxyWhenCompleteHandler(session, r, t))
                .isCancelled()) {
            throw new CancellationException("Canceled task");
        }
    }

    private static void proxyWhenCompleteHandler(@NotNull final HttpSession session,
                                                 @NotNull final Response r,
                                                 final Throwable t) {
        if (t == null) {
            sendResponse(session, r);
        } else {
            throw new RuntimeException("Logic error. t must be null", t);
        }
    }
}
