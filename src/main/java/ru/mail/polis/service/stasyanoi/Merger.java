package ru.mail.polis.service.stasyanoi;

import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        final List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == 200)
                .collect(Collectors.toList());
        final List<Response> emptyResponses = responses.stream()
                .filter(response -> response.getStatus() == 404)
                .collect(Collectors.toList());

        final Response responseHttp;

        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final boolean hasGoodResponses = !goodResponses.isEmpty();
            if (hasGoodResponses) {
                final List<Pair<Long, Response>> resps = Stream.concat(emptyResponses.stream(), goodResponses.stream())
                        .filter(response -> response.getHeader("Time: ") != null)
                        .map(response -> new Pair<>(Long.parseLong(response.getHeader("Time: ")), response))
                        .collect(Collectors.toList());
                final Map<Long, Response> map = new TreeMap<>();
                resps.forEach(pair -> map.put(pair.getValue0(), pair.getValue1()));
                final ArrayList<Map.Entry<Long, Response>> entries = new ArrayList<>(map.entrySet());
                responseHttp = entries.get(entries.size() - 1).getValue();
            } else if (emptyResponses.size() >= ack) {
                responseHttp = Util.getResponseWithNoBody(Response.NOT_FOUND);
            } else {
                responseHttp = Util.getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
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
    public static Response getEndResponsePutAndDelete(final List<Response> responses,
                                                      final Integer ack,
                                                      final int status,
                                                      final Map<Integer, String> nodeMapping) {
        final Response responseHttp;
        final List<Response> goodResponses = responses.stream()
                .filter(response -> response.getStatus() == status)
                .collect(Collectors.toList());
        if (nodeMapping.size() < ack || ack == 0) {
            responseHttp = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            if (goodResponses.size() >= ack) {
                if (status == 202) {
                    responseHttp = Util.getResponseWithNoBody(Response.ACCEPTED);
                } else {
                    responseHttp = Util.getResponseWithNoBody(Response.CREATED);
                }
            } else {
                responseHttp = Util.getResponseWithNoBody(Response.GATEWAY_TIMEOUT);
            }
        }
        return responseHttp;
    }
}
