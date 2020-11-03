package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MergeResponses {

    private final int ack;
    @NotNull
    final Collection<Response> responses;

    public MergeResponses(@NotNull final Collection<Response> responses, final int ack) {
        this.responses = responses;
        this.ack = ack;
    }

    Response mergeGetResponses() {
        int numNotFoundResponses = 0;
        boolean hasTombstone = false;
        long lastGeneration = 0;
        Response last = new Response(Response.NOT_FOUND, Response.EMPTY);
        for (final Response response : responses) {
            if (response.getStatus() == 404) {
                numNotFoundResponses++;
            } else if (response.getStatus() == 200) {
                final String head = response.getHeader("timestamp: ");
                final long generation = Long.parseLong(head);
                if (lastGeneration > generation || lastGeneration == 0) {
                    lastGeneration = generation;
                    last = response;
                }
                if (response.getHeader("tombstone: ").equals("true")) {
                    hasTombstone = true;
                }
            }
        }
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
        if (hasTombstone || responses.size() == numNotFoundResponses) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return Response.ok(last.getBody());

    }

    Response mergePutResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    Response mergeDeleteResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
