package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;
import static java.util.Comparator.comparingLong;

public class ResponseMerger {

    private final Util util;

    public ResponseMerger(final Util util) {
        this.util = util;
    }

    /**
     * Merge get responses.
     *
     * @param responses - responses to merge.
     * @param ack - ack.
     * @param from - from.
     * @return - merged response.
     */
    public Response mergeGetResponses(final List<Response> responses,
                                             final int ack,
                                             final int from) {
        final Response responseHttp;
        if (from < ack || ack == 0) {
            responseHttp = util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            boolean hasGoodResponses = false;
            int notFoundResponses = 0;
            final List<Response> validResponses = new ArrayList<>();
            for (final Response response : responses) {
                if (response.getStatus() == 200) {
                    hasGoodResponses = true;
                } else if (response.getStatus() == 404) {
                    notFoundResponses++;
                }
                if ((response.getStatus() == 200 || response.getStatus() == 404) &&
                        (response.getHeader(Constants.timestampHeaderName) != null)) {
                    validResponses.add(response);
                }
            }
            responseHttp = mergeGetInternal(ack, hasGoodResponses, notFoundResponses, validResponses);
        }
        return responseHttp;
    }

    private Response mergeGetInternal(final int ack, final boolean hasGoodResponses,
                                      final int notFoundResponses, final List<Response> validResponses) {
        final Response responseHttp;
        if (hasGoodResponses) {
            validResponses.sort(comparingLong(response ->
                    parseLong(response.getHeader(Constants.timestampHeaderName))));
            responseHttp = validResponses.get(validResponses.size() - 1);
        } else {
            if (notFoundResponses >= ack) {
                responseHttp = util.responseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = util.responseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }

    /**
     * Merge put and delete responses.
     *
     * @param responses - responses to merge.
     * @param ack = ack.
     * @param goodStatus - good status.
     * @param from = from.
     * @return - merged response.
     */
    public Response mergePutDeleteResponses(final List<Response> responses,
                                                   final int ack,
                                                   final int goodStatus,
                                                   final int from) {
        final Response responseHttp;
        if (ack > from || ack == 0) {
            responseHttp = util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final List<Response> goodResponses = new ArrayList<>();
            for (final Response response : responses) {
                if (response.getStatus() == goodStatus) {
                    goodResponses.add(response);
                }
            }
            if (goodResponses.size() >= ack) {
                if (goodStatus == 202) {
                    responseHttp = util.responseWithNoBody(Response.ACCEPTED);
                } else {
                    responseHttp = util.responseWithNoBody(Response.CREATED);
                }
            } else {
                responseHttp = util.responseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }
}
