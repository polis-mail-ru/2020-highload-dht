package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
        List<Response> responses = new ArrayList<>();
        var completableFutures = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>(
                        new Pair<>(asyncHttpClient, nodeHost.getValue()),
                        getNewRequest(request, port)))
                .map(clientRequest -> {
                    Pair<HttpClient, String> clientAndHost = clientRequest.getValue0();
                    HttpClient client = clientAndHost.getValue0();
                    String host = clientAndHost.getValue1();
                    Request oneNioRequest = clientRequest.getValue1();
                    HttpRequest javaRequest = Util.getJavaRequest(oneNioRequest, host);
                    return client.sendAsync(javaRequest, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApplyAsync(Util::getOneNioResponse)
                            .handle((response, throwable) -> {
                                if (throwable != null) {
                                    return Util.responseWithNoBody(Response.INTERNAL_ERROR);
                                } else {
                                    return response;
                                }
                            })
                            .thenAcceptAsync(responses::add);
                })
                .toArray(CompletableFuture[]::new);



        CompletableFuture.allOf(completableFutures).join();


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
        if (request.getQueryString().contains("&reps=false")) {
            newPath = path + "?" + queryString;
        } else {
            newPath = path + "?" + queryString + "&reps=false";
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
