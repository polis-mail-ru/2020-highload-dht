package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Stream.concat;
import static ru.mail.polis.service.stasyanoi.Util.*;

public final class Merger {

    private Merger() {

    }

    /**
     * Merge get responses.
     *
     * @param responses - responses to merge.
     * @param ack - ack.
     * @param nodeMapping - nodes.
     * @return - merged response.
     */
    @NotNull
    public static Response mergeGetResponses(final List<Response> responses,
                                             final Integer ack,
                                             final Map<Integer, String> nodeMapping) {


        final Response responseHttp;

        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
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
                responseHttp = responseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = responseWithNoBody(Response.GATEWAY_TIMEOUT);
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
    @NotNull
    public static Response mergePutDeleteResponses(final List<Response> responses,
                                                   final Integer ack,
                                                   final int status,
                                                   final Map<Integer, String> nodeMapping) {
        final Response responseHttp;
        if (ack > nodeMapping.size() || ack == 0) {
            responseHttp = responseWithNoBody(Response.BAD_REQUEST);
        } else {
            final List<Response> goodResponses = responses.stream()
                    .filter(response -> response.getStatus() == status)
                    .collect(Collectors.toList());
            if (goodResponses.size() >= ack) {
                if (status == 202) {
                    responseHttp = responseWithNoBody(Response.ACCEPTED);
                } else {
                    responseHttp = responseWithNoBody(Response.CREATED);
                }
            } else {
                responseHttp = responseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }
}
