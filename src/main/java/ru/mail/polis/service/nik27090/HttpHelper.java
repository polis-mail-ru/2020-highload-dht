package ru.mail.polis.service.nik27090;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpHelper {
    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

    private static final String NOT_ENOUGH_REPLICAS = "Not enough replicas error with ack: {}, from: {}";

    /**
     * Calculate result response.
     *
     * @param session               - session
     * @param sizeNotFailedResponse - size good response
     * @param ackFrom               - ack/from
     * @param goodResponse          - good response
     */
    public void calculateResponse(final HttpSession session,
                                  final int sizeNotFailedResponse,
                                  final AckFrom ackFrom,
                                  final Response goodResponse) {
        if (sizeNotFailedResponse < ackFrom.getAck()) {
            log.error(NOT_ENOUGH_REPLICAS, ackFrom.getAck(), ackFrom.getFrom());
            sendResponse(session, new Response(Response.GATEWAY_TIMEOUT, ("Not enough replicas error with ack: "
                    + ackFrom.getAck() + ", from: " + ackFrom.getFrom()).getBytes(UTF_8)));
        } else {
            sendResponse(session, goodResponse);
        }
    }

    /**
     * Add response in session.
     *
     * @param session  - current session
     * @param response - response of session
     */
    public void sendResponse(@NotNull final HttpSession session, @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error("Can't send response", e);
        }
    }

    /**
     * Proxy request to node.
     *
     * @param node         - node
     * @param request      - request
     * @param nodeToClient - list nodes of cluster
     * @return - response from node
     */
    @NotNull
    public Response proxy(
            @NotNull final String node,
            @NotNull final Request request,
            @NotNull final Map<String, HttpClient> nodeToClient) {
        request.addHeader("X-Proxy-For: " + node);
        try {
            return nodeToClient.get(node).invoke(request);
        } catch (InterruptedException | PoolException | HttpException | IOException e) {
            log.error("Can't proxy request", e);
            return new Response(Response.INTERNAL_ERROR);
        }
    }
}
