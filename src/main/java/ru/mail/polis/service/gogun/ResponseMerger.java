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

    @NotNull
    private Response getLatest(final Response response, Response latestResponse) {
        final long timestamp = Long.parseLong(response.getHeader(AsyncServiceImpl.TIMESTAMP_HEADER));
        final long latestTimestamp = Long.parseLong(latestResponse.getHeader(AsyncServiceImpl.TIMESTAMP_HEADER));
        if (timestamp > latestTimestamp) {
            latestResponse = response;
        }

        return latestResponse;
    }

    public Response mergeGetResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        int notFoundResponsesCount = 0;
        Response latestResponse = new Response("");
        latestResponse.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + Long.MIN_VALUE);
        for (final Response response : responses) {
            switch (response.getStatus()) {
                case 404:
                    if (response.getHeader(AsyncServiceImpl.TOMBSTONE_HEADER).equals("true")) {
                        latestResponse = getLatest(response, latestResponse);
                    } else {
                        notFoundResponsesCount++;
                    }
                    break;
                case 200:
                    latestResponse = getLatest(response, latestResponse);
                    break;
                default:
                    break;
            }
        }

        if (responses.size() == notFoundResponsesCount
                || latestResponse.getStatus() == 404) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(latestResponse.getBody());
    }

    public Response mergePutResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    public Response mergeDeleteResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
