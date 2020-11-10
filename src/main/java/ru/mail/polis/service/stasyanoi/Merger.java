package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Stream.concat;

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
            final List<Response> goodResponses = responses.stream()
                    .filter(response -> response.getStatus() == 200)
                    .collect(Collectors.toList());
            final List<Response> emptyResponses = responses.stream()
                    .filter(response -> response.getStatus() == 404)
                    .collect(Collectors.toList());
            final boolean hasGoodResponses = !goodResponses.isEmpty();
            if (hasGoodResponses) {
                final List<Response> responsesTemp = concat(emptyResponses.stream(), goodResponses.stream())
                        .filter(response -> response.getHeader("Time: ") != null)
                        .sorted(comparingLong(response -> parseLong(response.getHeader("Time: "))))
                        .collect(Collectors.toList());
                responseHttp = responsesTemp.get(responsesTemp.size() - 1);
            } else if (emptyResponses.size() >= ack) {
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
            final List<Response> goodResponses = responses.stream()
                    .filter(response -> response.getStatus() == status)
                    .collect(Collectors.toList());
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
