package ru.mail.polis.service.stasyanoi;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Util {

    private Util() {

    }

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Returns an empty bodied response
     *
     * @param requestType - type of response to return
     * @return the response
     */
    @NotNull
    public static Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    /**
     * Sends 500 error.
     *
     * @param session - to which to send the error .
     * @param e - the exception that caused the error.
     */
    public static void sendErrorInternal(final HttpSession session,
                                   final IOException e) {
        try {
            logger.error(e.getMessage(), e);
            session.sendError("500", e.getMessage());
        } catch (IOException exception) {
            logger.error(e.getMessage(), e);
        }
    }

}
