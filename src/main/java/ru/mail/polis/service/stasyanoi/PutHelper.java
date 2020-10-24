package ru.mail.polis.service.stasyanoi;

import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

import static ru.mail.polis.service.stasyanoi.Merger.getEndResponsePutAndDelete;

public final class PutHelper {

    private static final String TRUE_VAL = "true";
    private static final String REPS = "reps";

    private PutHelper() {

    }

    /**
     * Get put replicas.
     *
     * @param request - request to replicate.
     * @param tempNodeMapping - nodes that can hav the replicas.
     * @param responseHttpCurrent - this server responseto request.
     * @param replicationDefaults - RF defaults.
     * @param nodeMapping - nodes
     * @param port - this server port.
     * @return - returned response.
     */
    public static Response getPutReplicaResponse(final Request request,
                                           final Map<Integer, String> tempNodeMapping,
                                           final Response responseHttpCurrent,
                                           final List<String> replicationDefaults,
                                           final Map<Integer, String> nodeMapping,
                                           final int port) {
        Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom =
                    Util.getAckFrom(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses =
                    GetHelper.getResponsesInternal(responseHttpCurrent,
                            tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = getEndResponsePutAndDelete(responses, ack, 201, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }
}
