package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ResponseMerger {

    private final int ack;
    @NotNull
    private final Collection<Response> responses;

    /**
     * This class provides merging all responses from nodes to one, that client will get.
     *
     * @param responses list of responses
     * @param ack       number of responses to say, that answer is correct
     */
    public ResponseMerger(@NotNull final Collection<Response> responses, final int ack) {
        this.responses = responses;
        this.ack = ack;
        this.responses.removeIf((e) -> e.getStatus() == 500);
    }

    @NotNull
    private Response getLatest(final Response response, final Response latestResponse, final String timestamp) {
        final long timestampInNumber = Long.parseLong(timestamp);
        final long latestTimestamp = Long.parseLong(latestResponse.getHeader(AsyncServiceImpl.TIMESTAMP_HEADER));
        Response latestCopy = latestResponse;
        if (timestampInNumber > latestTimestamp) {
            latestCopy = response;
        }

        return latestCopy;
    }

    Response mergeGetResponses() {
        if (responses.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        int notFoundResponsesCount = 0;
        Response latestResponse = new Response("");
        latestResponse.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + Long.MIN_VALUE);
        for (final Response response : responses) {
            final String timestamp = response.getHeader(AsyncServiceImpl.TIMESTAMP_HEADER);
            switch (response.getStatus()) {
                case 404:
                    if (timestamp.equals(AsyncServiceImpl.ABSENT)) {
                        notFoundResponsesCount++;
                    } else {
                        latestResponse = getLatest(response, latestResponse, timestamp);
                    }
                    break;
                case 200:
                    latestResponse = getLatest(response, latestResponse, timestamp);
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
