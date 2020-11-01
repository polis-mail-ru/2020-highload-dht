package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GetMethodServer extends ConstantsServer {

    public GetMethodServer(final DAO dao,
                           final HttpServerConfig config,
                           final Set<String> topology) throws IOException {
        super(dao, config, topology);
    }

    /**
     * Ger replicas.
     *
     * @param responseHttpTemp - current server response.
     * @param tempNodeMapping - nodes for potential replication
     * @param from - RF replicas ack from
     * @param request - request to replicate
     * @param port - this server port.
     * @return - list of replica responses
     */
    public List<Response> getResponsesFromReplicas(final Response responseHttpTemp,
                                                   final Map<Integer, String> tempNodeMapping,
                                                   final int from,
                                                   final Request request,
                                                   final int port) {
        final List<Response> responses = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>(
                        new HttpClient(new ConnectionString(nodeHost.getValue())),
                        getNewRequest(request, port)))
                .map(clientRequest -> {
                    try (HttpClient value0 = clientRequest.getValue0()) {
                        return value0.invoke(clientRequest.getValue1());
                    } catch (InterruptedException | PoolException | IOException | HttpException e) {
                        return Util.responseWithNoBody(Response.INTERNAL_ERROR);
                    }
                })
                .collect(Collectors.toList());

        responses.add(responseHttpTemp);
        return responses;
    }

    /**
     * Create new request.
     *
     * @param request - old request.
     * @param port - this server port.
     * @return - new Request.
     */
    @NotNull
    public Request getNewRequest(final Request request, final int port) {
        final String path = request.getPath();
        final String queryString = request.getQueryString();
        final String newPath = path + "/rep?" + queryString;
        final Request requestNew = getCloneRequest(request, newPath, port);
        requestNew.setBody(request.getBody());
        return requestNew;
    }

    /**
     * Create no replication request.
     *
     * @param request - old request.
     * @param port - this server port.
     * @return - new request.
     */
    @NotNull
    public Request getNoRepRequest(final Request request,
                                          final int port) {
        final String path = request.getPath();
        final String queryString = request.getQueryString();
        final String newPath;
        if (request.getHeader(REPS) == null) {
            newPath = path + "?" + queryString + "&reps=false";
        } else {
            newPath = path + "?" + queryString;
        }
        final Request noRepRequest = getCloneRequest(request, newPath, port);
        noRepRequest.setBody(request.getBody());
        return noRepRequest;
    }

    @NotNull
    private Request getCloneRequest(final Request request,
                                           final String newPath,
                                           final int thisServerPort) {
        final Request noRepRequest = new Request(request.getMethod(), newPath, true);
        Arrays.stream(request.getHeaders())
                .filter(Objects::nonNull)
                .filter(header -> !header.contains("Host: "))
                .forEach(noRepRequest::addHeader);
        noRepRequest.addHeader("Host: localhost:" + thisServerPort);
        return noRepRequest;
    }

    /**
     * Get repsponse for get request.
     *
     * @param id - key.
     * @param dao - dao to use.
     * @return - response.
     * @throws IOException - throw if problems with I|O occur.
     */
    public Response getResponseIfIdNotNull(final ByteBuffer id,
                                                  final DAO dao) throws IOException {
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            final Pair<byte[], byte[]> bodyTimestamp = Util.getTimestamp(bytes);
            final byte[] newBody = bodyTimestamp.getValue0();
            final byte[] time = bodyTimestamp.getValue1();
            final Response okResponse = Response.ok(newBody);
            Util.addTimestampHeader(time, okResponse);
            return okResponse;
        } catch (NoSuchElementException e) {
            final byte[] deleteTime = dao.getDeleteTime(id);
            if (deleteTime.length == 0) {
                return Util.responseWithNoBody(Response.NOT_FOUND);
            } else {
                final Response deletedResponse = Util.responseWithNoBody(Response.NOT_FOUND);
                Util.addTimestampHeader(deleteTime, deletedResponse);
                return deletedResponse;
            }
        }
    }

    /**
     * Get replica request for GET.
     *
     * @param request - request to replicate.
     * @param tempNodeMapping - node mapping for replication
     * @param responseHttpCurrent - this server get response.
     * @param nodeMapping - nodes
     * @param port - this server port.
     * @return - the replica response.
     */
    public Response getReplicaGetResponse(final Request request,
                                                 final Map<Integer, String> tempNodeMapping,
                                                 final Response responseHttpCurrent,
                                                 final Map<Integer, String> nodeMapping,
                                                 final int port) {
        final Response responseHttp;
        if (request.getParameter(REPS, TRUE_VAL).equals(TRUE_VAL)) {
            final Pair<Integer, Integer> ackFrom = Util.ackFromPair(request, replicationDefaults, nodeMapping);
            final int from = ackFrom.getValue1();
            final List<Response> responses = getResponsesFromReplicas(responseHttpCurrent,
                    tempNodeMapping, from - 1, request, port);
            final Integer ack = ackFrom.getValue0();
            responseHttp = Merger.mergeGetResponses(responses, ack, nodeMapping);
        } else {
            responseHttp = responseHttpCurrent;
        }
        return responseHttp;
    }
}
