package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;
import static java.util.Comparator.comparingLong;

public class Merger {

    private final Util util;

    public Merger(final Util util) {
        this.util = util;
    }

    /**
     * Merge get responses.
     *
     * @param responses - responses to merge.
     * @param ack - ack.
     * @param nodeMapping - nodes.
     * @return - merged response.
     */
    public Response mergeGetResponses(final List<Response> responses,
                                             final Integer ack,
                                             final Map<Integer, String> nodeMapping) {
        final Response responseHttp;
        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            responseHttp = mergeGetInternal(responses, ack);
        }
        return responseHttp;
    }

    private Response mergeGetInternal(final List<Response> responses, final Integer ack) {
        final Response responseHttp;
        boolean hasGoodResponses = false;
        int notFoundResponses = 0;
        final List<Response> validResponses = new ArrayList<>();
        for (final Response response : responses) {
            if ((response.getStatus() == 200 || response.getStatus() == 404) && response.getHeader("Time: ")
                    != null) {
                if (response.getStatus() == 200 && !hasGoodResponses) {
                    hasGoodResponses = true;
                } else if (response.getStatus() == 404) {
                    notFoundResponses++;
                }
                validResponses.add(response);
            }
        }
        responseHttp = reduceGetResponses(ack, hasGoodResponses, notFoundResponses, validResponses);
        return responseHttp;
    }

    private Response reduceGetResponses(final Integer ack, final boolean hasGoodResponses,
                                        final int notFoundResponses, final List<Response> validResponses) {
        final Response responseHttp;
        if (hasGoodResponses) {
            validResponses.sort(comparingLong(response -> parseLong(response.getHeader("Time: "))));
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
     * @param status - good status.
     * @param nodeMapping - nodes.
     * @return - merged response.
     */
    public Response mergePutDeleteResponses(final List<Response> responses,
                                                   final Integer ack,
                                                   final int status,
                                                   final Map<Integer, String> nodeMapping) {
        final Response responseHttp;
        if (ack > nodeMapping.size() || ack == 0) {
            responseHttp = util.responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final List<Response> goodResponses = new ArrayList<>();
            for (final Response response : responses) {
                if (response.getStatus() == status) {
                    goodResponses.add(response);
                }
            }
            if (goodResponses.size() >= ack) {
                if (status == 202) {
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
