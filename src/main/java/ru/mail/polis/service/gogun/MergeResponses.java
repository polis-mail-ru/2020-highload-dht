package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergeResponses {

    private final int ack;
    @NotNull
    final List<Response> responses;

    public MergeResponses(@NotNull final List<Response> responses, final int ack) {
        this.responses = responses;
        this.ack = ack;
    }

    Response mergeGetResponses() {
        int notFoundResponsesCount = 0;
        long latestTimestamp = Long.MIN_VALUE;
        Response latestResponse = new Response("");
        for (final Response response : responses) {
            switch (response.getStatus()) {
                case 404:
                    notFoundResponsesCount++;
                    break;
                case 200:
                    final long timestamp = Long.parseLong(response.getHeader("timestamp: "));
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                        latestResponse = response;
                    }
                    break;
                default:
                    break;
            }
        }
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
        if (latestResponse.getHeader("tombstone: ").equals("true") || responses.size() == notFoundResponsesCount) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(latestResponse.getBody());
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
