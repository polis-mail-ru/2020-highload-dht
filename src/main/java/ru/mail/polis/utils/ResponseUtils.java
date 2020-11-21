package ru.mail.polis.utils;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.mrsandman5.ServiceImpl;
import ru.mail.polis.service.mrsandman5.handlers.DeleteBodyHandler;
import ru.mail.polis.service.mrsandman5.handlers.GetBodyHandler;
import ru.mail.polis.service.mrsandman5.handlers.PutBodyHandler;
import ru.mail.polis.service.mrsandman5.replication.Entry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class ResponseUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public static final String ENTITY = "/v0/entity";
    public static final int TIMEOUT_MILLIS = 2000;
    public static final String PROXY = "X-Proxy-For";
    public static final String TIMESTAMP = "Timestamp";
    public static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    public static final String EXPIRES = "Expires";

    public static final DateTimeFormatter expirationFormat =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

    private ResponseUtils() {
    }

    /** Send session response to service.
     * @param session - current HTTP session.
     * @param response - response to request.
     * */
    public static void sendResponse(@NotNull final HttpSession session,
                                    @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            try {
                log.error("Unable to send response", e);
                session.sendError(Response.INTERNAL_ERROR, null);
            } catch (IOException ex) {
                log.error("Unable to send error", e);
            }
        }
    }

    /** Create request builder for current client.
     * @param node - current client.
     * @param id - request id.
     * */
    @NotNull
    public static HttpRequest.Builder requestForReplica(@NotNull final String node,
                                                        @NotNull final String id) {
        final String uri = node + ENTITY + "?id=" + id;
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header(PROXY, "True")
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS));
    }

    public static void sendEmptyResponse(@NotNull final HttpSession session,
                                         @NotNull final String code) {
        sendResponse(session, emptyResponse(code));
    }

    public static void sendNonEmptyResponse(@NotNull final HttpSession session,
                                            @NotNull final String code,
                                            final byte[] values) {
        sendResponse(session, nonemptyResponse(code, values));
    }

    @NotNull
    public static Response emptyResponse(@NotNull final String code) {
        return new Response(code, Response.EMPTY);
    }

    @NotNull
    public static Response nonemptyResponse(@NotNull final String code,
                                            final byte[] values) {
        return new Response(code, values);
    }

    public static String getTimestamp(@NotNull final Entry entry) {
        return TIMESTAMP + ": " + entry.getTimestamp();
    }

    public static String getExpires(@NotNull final Entry entry) {
        return EXPIRES + ": " + entry.getExpires();
    }

    /** GET response from Entry.*/
    public static CompletableFuture<Entry> getResponse(@NotNull final Map<String, HttpClient> httpClients,
                                                          @NotNull final String node,
                                                          @NotNull final String id,
                                                          @NotNull final ExecutorService executor) {
        final HttpRequest request = ResponseUtils.requestForReplica(node, id)
                .GET()
                .build();
        return httpClients.get(node)
                .sendAsync(request, GetBodyHandler.INSTANCE)
                .thenApplyAsync(HttpResponse::body, executor);
    }

    /** PUT response from Entry.*/
    public static CompletableFuture<Response> putResponse(@NotNull final Map<String, HttpClient> httpClients,
                                                          @NotNull final String node,
                                                          @NotNull final String id,
                                                          @NotNull final byte[] value,
                                                          @NotNull final ExecutorService executor) {
        final HttpRequest request = ResponseUtils.requestForReplica(node, id)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(value))
                .build();
        return httpClients.get(node)
                .sendAsync(request, PutBodyHandler.INSTANCE)
                .thenApplyAsync(r -> ResponseUtils.emptyResponse(Response.CREATED), executor);
    }

    /** DELETE response from Entry.*/
    public static CompletableFuture<Response> deleteResponse(@NotNull final Map<String, HttpClient> httpClients,
                                                          @NotNull final String node,
                                                          @NotNull final String id,
                                                          @NotNull final ExecutorService executor) {
        final HttpRequest request = ResponseUtils.requestForReplica(node, id)
                .DELETE()
                .build();
        return httpClients.get(node)
                .sendAsync(request, DeleteBodyHandler.INSTANCE)
                .thenApplyAsync(r -> ResponseUtils.emptyResponse(Response.ACCEPTED), executor);
    }
}
