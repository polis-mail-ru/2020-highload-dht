package ru.mail.polis.service.suhova;

import one.nio.http.Response;

import java.util.List;

public class Consensus {
    /**
     * Resolves conflicts among responses received from a GET request.
     *
     * @param responses - list of all responses
     * @param ack       - ack
     * @return resulting response
     */
    public static Response get(List<Response> responses, int ack) {
        int count = 0;
        int count404 = 0;
        boolean deleted = false;
        long lastVersion = 0;
        Response lastValue = new Response(Response.NOT_FOUND, Response.EMPTY);
        for (Response response : responses) {
            if (response.getStatus() == 200) {
                count++;
                long version = Long.parseLong(response.getHeader("version"));
                if (Boolean.parseBoolean(response.getHeader("isTombstone"))) {
                    deleted = true;
                }
                if (lastVersion > version || lastVersion == 0) {
                    lastVersion = version;
                    lastValue = response;
                }
            } else if (response.getStatus() == 404) {
                count404++;
                count++;
            }
        }
        if (count >= ack) {
            if (deleted || count == count404) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } else {
                return Response.ok(lastValue.getBody());
            }
        } else {
            return new Response("504 Not Enough Replicas", Response.EMPTY);
        }
    }

    /**
     * Resolves conflicts among responses received from a PUT request.
     *
     * @param responses - list of all responses
     * @param ack       - ack
     * @return resulting response
     */
    public static Response put(List<Response> responses, int ack) {
        int ackCount = 0;
        for (Response response : responses) {
            if (response.getStatus() == 201) {
                ackCount++;
            }
        }
        if (ackCount >= ack) {
            return new Response(Response.CREATED, Response.EMPTY);
        } else {
            return new Response("504 Not Enough Replicas", Response.EMPTY);
        }
    }

    /**
     * Resolves conflicts among responses received from a DELETE request.
     *
     * @param responses - list of all responses
     * @param ack       - ack
     * @return resulting response
     */
    public static Response delete(List<Response> responses, int ack) {
        int ackCount = 0;
        for (Response response : responses) {
            if (response.getStatus() == 202) {
                ackCount++;
            }
        }
        if (ackCount >= ack) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response("504 Not Enough Replicas", Response.EMPTY);
        }
    }
}
