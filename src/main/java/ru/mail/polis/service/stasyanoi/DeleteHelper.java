package ru.mail.polis.service.stasyanoi;

import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ru.mail.polis.service.stasyanoi.Merger.getEndResponsePutAndDelete;

public final class DeleteHelper {

    private static final String TRUE_VAL = "true";
    private static final String REPS = "reps";
    private static final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");


    private DeleteHelper() {

    }

    public static Response getDeleteReplicaResponse(final Request request,
                                              final Map<Integer, String> tempNodeMapping,
                                              final Response responseHttpCurrent,
                                              final Map<Integer, String> nodeMapping,
                                              final int port) {
        final Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom =
                    Util.getAckFrom(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses =
                    GetHelper.getResponsesInternal(responseHttpCurrent,
                            tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = getEndResponsePutAndDelete(responses, ack, 202, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }
}
