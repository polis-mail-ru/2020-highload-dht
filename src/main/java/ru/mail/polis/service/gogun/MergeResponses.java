package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergeResponses {

    static Response mergeGetResponses(@NotNull final List<Response> responses, final int ask) {
        int numNotFoundResponses = 0;
        boolean hasTombstone = false;
        long lastGeneration = 0;
        Response last = new Response(Response.NOT_FOUND, Response.EMPTY);
        for (final Response response : responses) {
            if (response.getStatus() == 404) {
                numNotFoundResponses++;
            } else if (response.getStatus() == 200) {
                final long generation = Long.parseLong(response.getHeader("timestamp: "));
                if (lastGeneration > generation || lastGeneration == 0) {
                    lastGeneration = generation;
                    last = response;
                }
                if (response.getHeader("tombstone: ").equals("true")) {
                    hasTombstone = true;
                }
            }
        }
        if (responses.size() < ask) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
        if (hasTombstone || responses.size() == numNotFoundResponses) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return Response.ok(last.getBody());

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
