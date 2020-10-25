package ru.mail.polis.service.zvladn7;

import com.google.common.primitives.Longs;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

class ConflictResolver {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolver.class);

    private ConflictResolver() {
    }

    /**
     * Resolve responses from replicas to send fresh data to the client.
     *
     * @param responses      - responses from the active nodes.
     *                       There are 3 types of it:
     *                       - 200 OK with value and timestamp
     *                       - 404 NOT FOUND without data but with timestamp
     *                       - 404 NOT FOUND with empty response body(there was not data with requested key on the node)
     * @param replicasHolder - replication factor which where send as http parameter or default
     * @param session        - http server session
     * @throws IOException - if cannot send response via session
     */
    static void resolveGetAndSend(@NotNull final List<Response> responses,
                                  @NotNull final ReplicasHolder replicasHolder,
                                  @NotNull final HttpSession session) throws IOException {
        if (responses.size() < replicasHolder.ack) {
            sendNotEnoughReplicasResponse(session, replicasHolder);
            return;
        }

        Response theMostFreshedResponse = responses.get(0);
        long theMostFreshedTimestamp = getResponseTimestamp(theMostFreshedResponse);
        boolean first = true;
        for (final Response next : responses) {
            if (first) {
                first = false;
                continue;
            }
            final long responseTimestamp = getResponseTimestamp(next);
            if (responseTimestamp > theMostFreshedTimestamp) {
                theMostFreshedTimestamp = responseTimestamp;
                theMostFreshedResponse = next;
            }
        }

        if (theMostFreshedResponse.getStatus() == 200) {
            final byte[] body = getValueBody(theMostFreshedResponse);
            session.sendResponse(Response.ok(body));
        } else {
            session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
        }
    }


    /**
     * Resolve responses from replicas to send fresh data to the client.
     *
     * @param responses      - responses from the active nodes.
     * @param replicasHolder - replication factor which where send as http parameter or default
     * @param session        - http server session
     */
    static void resolveChangesAndSend(@NotNull final List<Response> responses,
                                     @NotNull final ReplicasHolder replicasHolder,
                                     @NotNull final HttpSession session,
                                     @NotNull final Boolean isDelete) {
        try {
            if (responses.size() < replicasHolder.ack) {
                sendNotEnoughReplicasResponse(session, replicasHolder);
                return;
            }

            if (isDelete) {
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } else {
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            }
        } catch (IOException ex) {
            log.error("Cannot send response on delete or upsert operation", ex);
        }
    }

    private static byte[] getValueBody(@NotNull final Response response) {
        final byte[] body = response.getBody();
        final byte[] valueBody = new byte[body.length - Long.BYTES];
        System.arraycopy(body, 0, valueBody, 0, valueBody.length);
        return valueBody;
    }

    private static long getResponseTimestamp(@NotNull final Response response) {
        final byte[] body = response.getBody();
        if (body.length == 0) {
            return -1;
        }
        if (response.getStatus() == 200) {
            final byte[] timestampBytes = new byte[Long.BYTES];
            System.arraycopy(body, body.length - Long.BYTES, timestampBytes, 0, Long.BYTES);
            return Longs.fromByteArray(timestampBytes);
        } else {
            return Longs.fromByteArray(body);
        }
    }

    private static void sendNotEnoughReplicasResponse(@NotNull final HttpSession session,
                                                      @NotNull final ReplicasHolder replicasHolder) throws IOException {
        log.error("Not enough replicas error with ack: {}, from: {}", replicasHolder.ack, replicasHolder.from);
        session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
    }
}
