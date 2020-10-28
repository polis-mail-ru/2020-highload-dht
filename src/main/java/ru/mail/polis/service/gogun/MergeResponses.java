package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergeResponses {

    static Response mergeGetResponses(@NotNull final List<Response> responses, final int ask) {
        if (responses.size() < ask) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        Response theMostFreshedResponse = responses.get(0);
        long theMostFreshedTimestamp;
        if (theMostFreshedResponse.getHeader("timestamp: ") == null) {
            theMostFreshedTimestamp = -1;
        } else {
            theMostFreshedTimestamp = Long.parseLong(theMostFreshedResponse.getHeader("timestamp: "));
        }
        boolean first = true;
        for (final Response next : responses) {
            if (first) {
                first = false;
                continue;
            }
            long responseTimestamp;
            if (next.getHeader("timestamp: ") == null) {
                responseTimestamp = -1;
            } else {
                responseTimestamp = Long.parseLong(next.getHeader("timestamp: "));
            }
            if (responseTimestamp > theMostFreshedTimestamp) {
                theMostFreshedTimestamp = responseTimestamp;
                theMostFreshedResponse = next;
            }
        }

        if (theMostFreshedResponse.getStatus() == 200) {
            final byte[] body = (theMostFreshedResponse.getBody());
            return (Response.ok(body));
        } else {
            return (new Response(Response.NOT_FOUND, Response.EMPTY));
        }

    }

    static Response mergePutResponses(@NotNull final List<Response> responses, final int ask) {
        if (responses.size() < ask) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return (new Response(Response.CREATED, Response.EMPTY));
    }

    static Response mergeDeleteResponses(@NotNull final List<Response> responses, final int ask) {
        if (responses.size() < ask) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return (new Response(Response.ACCEPTED, Response.EMPTY));
    }
}
