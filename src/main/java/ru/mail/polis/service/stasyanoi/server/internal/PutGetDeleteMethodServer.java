package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PutGetDeleteMethodServer extends DeleteGetMethodServer {

    public PutGetDeleteMethodServer(final DAO dao,
                                    final HttpServerConfig config,
                                    final Set<String> topology) throws IOException {
        super(dao, config, topology);
    }

    /**
     * Get put replicas.
     *
     * @param request - request to replicate.
     * @param mappings - nodes that can have the replicas and the total amount nodes .
     * @param responseHttpCurrent - this server responseto request.
     * @param port - this server port.
     * @return - returned response.
     */
    public Response getPutReplicaResponse(final Request request,
                                                 final Pair<Map<Integer, String>, Map<Integer, String>> mappings,
                                                 final Response responseHttpCurrent,
                                                 final int port) {
        final Map<Integer, String> tempNodeMapping = mappings.getValue0();
        final Map<Integer, String> nodeMapping = mappings.getValue1();
        Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = Util.ackFromPair(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                            tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = Merger.getEndResponsePutAndDelete(responses, ack, 201, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }
}
