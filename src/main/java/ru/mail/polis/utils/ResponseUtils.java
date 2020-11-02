package ru.mail.polis.utils;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.mrsandman5.ServiceImpl;
import ru.mail.polis.service.mrsandman5.replication.Entry;

import java.io.IOException;

public final class ResponseUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public static final String ENTITY = "/v0/entity";
    public static final int TIMEOUT_MILLIS = 1000;
    public static final String PROXY = "X-Proxy-For";
    public static final String TIMESTAMP = "Timestamp";
    public static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";

    private ResponseUtils() {
    }

    /** Send seesion response to service.
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

    public static String getStatus(@NotNull final Response response) {
        return response.getHeaders()[0];
    }

    public static String getTimestamp(@NotNull final Entry entry) {
        return TIMESTAMP + ": " + entry.getTimestamp();
    }
}
