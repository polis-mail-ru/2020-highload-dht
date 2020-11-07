package ru.mail.polis.util;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.s3ponia.ProxyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static ru.mail.polis.util.Utility.sendResponse;

/**
 * Utility class for proxying requests to different nodes.
 */
public final class Proxy {
    public static final String PROXY_HEADER = "X-Proxy-From";

    private Proxy() {
    }

    /**
     * Proxying request.
     * @param request request to proxy
     * @param client destination point
     * @return Response from server
     */
    public static Response proxy(
            @NotNull final Request request,
            @NotNull final HttpClient client) throws ProxyException {
        try {
            request.addHeader(PROXY_HEADER + ":" + "proxied");
            return client.invoke(request);
        } catch (InterruptedException | HttpException | PoolException | IOException e) {
            throw new ProxyException("Error in proxying");
        }
    }

    /**
     * Get list of success responses from nodes.
     *
     * @param request     proxying request
     * @param httpClients array of httpclients to proxy
     * @return list of sucess responses
     */
    @NotNull
    public static List<Response> proxyReplicas(@NotNull final Request request,
                                               @NotNull final Collection<HttpClient> httpClients) {
        final List<Response> futureResponses = new ArrayList<>();

        for (final var httpClient : httpClients) {

            final Response response;
            try {
                response = proxy(request, httpClient);
            } catch (ProxyException e) {
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
        return futureResponses;
    }

    /**
     * Handle CompletableFuture Response on responding to proxy request.
     * @param session session for responding
     * @param proxyHandler asynchronous response on proxy request
     */
    public static void proxyHandle(@NotNull final HttpSession session,
                                   @NotNull final CompletableFuture<Response> proxyHandler) {
        if (proxyHandler
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
