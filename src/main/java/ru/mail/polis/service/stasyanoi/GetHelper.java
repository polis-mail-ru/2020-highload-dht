package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GetHelper {

    private static final String REPS = "reps";

    private GetHelper() {

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
    public static List<Response> getResponsesInternal(final Response responseHttpTemp,
                                                final Map<Integer, String> tempNodeMapping,
                                                final int from,
                                                final Request request,
                                                final int port) {
        final List<Response> responses = tempNodeMapping.entrySet()
                .stream()
                .limit(from)
                .map(nodeHost -> new Pair<>(
                        new HttpClient(new ConnectionString(nodeHost.getValue())), getNewRequest(request, port)))
                .map(clientRequest -> {
                    try {
                        final Response invoke = clientRequest.getValue0().invoke(clientRequest.getValue1());
                        clientRequest.getValue0().close();
                        return invoke;
                    } catch (InterruptedException | PoolException | IOException | HttpException e) {
                        return Util.getResponseWithNoBody(Response.INTERNAL_ERROR);
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
    public static Request getNewRequest(final Request request, final int port) {
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
    public static Request getNoRepRequest(final Request request,
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
    private static Request getCloneRequest(final Request request,
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
}
