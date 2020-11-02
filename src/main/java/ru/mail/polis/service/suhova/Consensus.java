package ru.mail.polis.service.suhova;

import one.nio.http.Response;

import java.util.List;

import static ru.mail.polis.service.suhova.DAOServiceMethods.TOMBSTONE;
import static ru.mail.polis.service.suhova.DAOServiceMethods.VERSION;

public final class Consensus {
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";

    private Consensus() {
    }

    /**
     * Resolves conflicts among responses received from a GET request.
     *
     * @param responses - list of all responses
     * @param acks      - acks
     * @return resulting response
     */
    public static Response get(final List<Response> responses, final int acks) {
        int count = 0;
        int count404 = 0;
        Response okValue = new Response(Response.NOT_FOUND, Response.EMPTY);
        long lastVersion = 0;
        for (final Response response : responses) {
            final int status = response.getStatus();
            if (status == 200) {
                count++;
                final long version = Long.parseLong(response.getHeader(VERSION));
                if (version > lastVersion) {
                    lastVersion = version;
                    okValue = response;
                }
            } else if (status == 404) {
                count404++;
                count++;
            }
        }
        if (count >= acks) {
            if (Boolean.parseBoolean(okValue.getHeader(TOMBSTONE)) || count == count404) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } else {
                return Response.ok(okValue.getBody());
            }
        } else {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
    }

    /**
     * Resolves conflicts among responses received from a PUT request.
     *
     * @param responses - list of all responses
     * @param acks      - ack
     * @return resulting response
     */
    public static Response put(final List<Response> responses, final int acks) {
        return successIfEnoughAcks(responses, acks, 201, Response.CREATED);
    }

    /**
     * Resolves conflicts among responses received from a DELETE request.
     *
     * @param responses - list of all responses
     * @param acks      - ack
     * @return resulting response
     */
    public static Response delete(final List<Response> responses, final int acks) {
        return successIfEnoughAcks(responses, acks, 202, Response.ACCEPTED);
    }

    private static Response successIfEnoughAcks(final List<Response> responses,
                                                final int acks,
                                                final int status,
                                                final String result) {
        int ackCount = 0;
        for (final Response response : responses) {
            if (response.getStatus() == status) {
                ackCount++;
            }
        }
        if (ackCount >= acks) {
            return new Response(result, Response.EMPTY);
        } else {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
    }
}
