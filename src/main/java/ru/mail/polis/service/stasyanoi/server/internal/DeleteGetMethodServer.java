package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static ru.mail.polis.service.stasyanoi.Merger.getEndResponsePutAndDelete;

public class DeleteGetMethodServer extends GetMethodServer {

    public DeleteGetMethodServer(HttpServerConfig config) throws IOException {
        super(config);
    }

    /**
     * Get response for delete replication.
     *
     * @param request - request to replicate.
     * @param tempNodeMapping - nodes that can get replicas.
     * @param responseHttpCurrent - this server response.
     * @param nodeMapping - nodes
     * @param port - this server port.
     * @return - response for delete replicating.
     */
    public Response getDeleteReplicaResponse(final Request request,
                                             final Map<Integer, String> tempNodeMapping,
                                             final Response responseHttpCurrent,
                                             final Map<Integer, String> nodeMapping,
                                             final int port) {
        final Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = getRF(request, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                    tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = getEndResponsePutAndDelete(responses, ack, 202, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }

    @NotNull
    private Pair<Integer, Integer> getRF(final Request request, final Map<Integer, String> nodeMapping) {
        return Util.ackFromPair(request, replicationDefaults, nodeMapping);
    }
}
