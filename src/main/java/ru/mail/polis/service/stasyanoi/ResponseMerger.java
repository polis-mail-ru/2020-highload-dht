package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.parseLong;
import static java.util.Comparator.comparingLong;

public final class ResponseMerger {

    private ResponseMerger() {

    }

    /**
     * Merge get responses.
     *
     * @param responses - responses to merge.
     * @param ack - ack.
     * @param from - from.
     * @return - merged response.
     */
    public static Response mergeGetResponses(final List<Response> responses,
                                             final int ack,
                                             final int from) {
        final Response responseHttp;
        if (from < ack || ack == 0) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
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
                if ((response.getStatus() == 200 || response.getStatus() == 404)
                        && (response.getHeader(Constants.TIMESTAMP_HEADER_NAME) != null)) {
                    validResponses.add(response);
                }
            }
            responseHttp = mergeGetInternal(ack, hasGoodResponses, notFoundResponses, validResponses);
        }
        return responseHttp;
    }

    private static Response mergeGetInternal(final int ack, final boolean hasGoodResponses,
                                      final int notFoundResponses, final List<Response> validResponses) {
        final Response responseHttp;
        if (hasGoodResponses) {
            validResponses.sort(comparingLong(ResponseMerger::getLongTimestamp));
            responseHttp = validResponses.get(validResponses.size() - 1);
        } else {
            if (notFoundResponses >= ack) {
                responseHttp = Util.responseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = Util.responseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }

    private static long getLongTimestamp(final Response response) {
        final String headerTimestamp = response.getHeader(Constants.TIMESTAMP_HEADER_NAME);
        return parseLong(headerTimestamp);
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
    public static Response mergePutDeleteResponses(final List<Response> responses,
                                                   final int ack,
                                                   final int goodStatus,
                                                   final int from) {
        final Response responseHttp;
        if (ack > from || ack == 0) {
            responseHttp = Util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final List<Response> goodResponses = new ArrayList<>();
            for (final Response response : responses) {
                if (response.getStatus() == goodStatus) {
                    goodResponses.add(response);
                }
            }
            if (goodResponses.size() >= ack) {
                if (goodStatus == 202) {
                    responseHttp = Util.responseWithNoBody(Response.ACCEPTED);
                } else {
                    responseHttp = Util.responseWithNoBody(Response.CREATED);
                }
            } else {
                responseHttp = Util.responseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }
}
