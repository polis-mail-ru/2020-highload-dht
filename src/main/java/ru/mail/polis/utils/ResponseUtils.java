package ru.mail.polis.utils;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.mrsandman5.ServiceImpl;

import java.io.IOException;

public final class ResponseUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    public static final String TIMEOUT = "?timeout=1000";
    public static final String PROXY = "Is-Proxy: True";
    public static final String TIMESTAMP = "Timestamp: ";
    //public static final String PATH = "/v0/entity?id=";
    public static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";

    private ResponseUtils(){}

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
}
