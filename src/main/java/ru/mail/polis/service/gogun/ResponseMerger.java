package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResponseMerger {

    private final int ack;
    @NotNull
    private final List<Response> responses;

    /**
     * This class provides merging all responses from nodes to one, that client will get.
     *
     * @param responses list of responses
     * @param ack       number of responses to say, that answer is correct
     */
    public ResponseMerger(@NotNull final List<Response> responses, final int ack) {
        this.responses = responses;
        this.ack = ack;
        this.responses.removeIf((e) -> e.getStatus() == 500);
    }

    Response mergeGetResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        int notFoundResponsesCount = 0;
        long latestTimestamp = Long.MIN_VALUE;
        Response latestResponse = new Response("");
        for (final Response response : responses) {
            switch (response.getStatus()) {
                case 404:
                    notFoundResponsesCount++;
                    break;
                case 200:
                    final long timestamp = Long.parseLong(response.getHeader(AsyncServiceImpl.TIMESTAMP_HEADER));
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                        latestResponse = response;
                    }
                    break;
                default:
                    break;
            }
        }

        if (responses.size() == notFoundResponsesCount
                || latestResponse.getHeader(AsyncServiceImpl.TOMBSTONE_HEADER).equals("true")) {
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
