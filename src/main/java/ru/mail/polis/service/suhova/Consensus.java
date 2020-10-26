package ru.mail.polis.service.suhova;

import one.nio.http.Response;

import java.util.List;

public final class Consensus {
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";

    private Consensus() {
    }

    /**
     * Resolves conflicts among responses received from a GET request.
     *
     * @param responses - list of all responses
     * @param ack       - ack
     * @return resulting response
     */
    public static Response get(final List<Response> responses, final int ack) {
        int count = 0;
        int count404 = 0;
        boolean isDeleted = false;
        Response okValue = new Response(Response.NOT_FOUND, Response.EMPTY);
        for (final Response response : responses) {
            final int status = response.getStatus();
            if (status == 200) {
                count++;
                if (Boolean.parseBoolean(response.getHeader("isTombstone"))) {
                    isDeleted = true;
                    continue;
                }
                okValue = response;
            } else if (status == 404) {
                count404++;
                count++;
            }
        }
        if (count >= ack) {
            if (isDeleted || count == count404) {
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
     * @param ack       - ack
     * @return resulting response
     */
    public static Response put(final List<Response> responses, final int ack) {
        return simpleResponse(responses, ack, 201, Response.CREATED);
    }

    /**
     * Resolves conflicts among responses received from a DELETE request.
     *
     * @param responses - list of all responses
     * @param ack       - ack
     * @return resulting response
     */
    public static Response delete(final List<Response> responses, final int ack) {
        return simpleResponse(responses, ack, 202, Response.ACCEPTED);
    }

    private static Response simpleResponse(final List<Response> responses,
                                           final int ack,
                                           final int status,
                                           final String result) {
        int ackCount = 0;
        for (final Response response : responses) {
            if (response.getStatus() == status) {
                ackCount++;
            }
        }
        if (ackCount >= ack) {
            return new Response(result, Response.EMPTY);
        } else {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
    }
}
