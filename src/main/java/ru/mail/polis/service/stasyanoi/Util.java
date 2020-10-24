package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    @NotNull
    public static Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    public static Pair<Integer, Integer> getAckFrom(Request request, List<String> replicationDefaults, Map<Integer, String> nodeMapping) {
        int ack;
        int from;
        String replicas = request.getParameter("replicas");
        if (replicas == null) {
            Optional<String[]> ackFrom = replicationDefaults.stream()
                    .map(replic -> replic.split("/"))
                    .filter(strings -> Integer.parseInt(strings[1]) == nodeMapping.size())
                    .findFirst();

            ack = Integer.parseInt(ackFrom.get()[0]);
            from = Integer.parseInt(ackFrom.get()[1]);
        } else {
            replicas = replicas.substring(1);
            ack = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 0));
            from = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 1));
        }

        return new Pair<>(ack, from);
    }

    public static Response routeRequest(final Request request, final int node, Map<Integer, String> nodeMapping, int nodeNum)
            throws IOException {

        logger.info(nodeMapping.get(nodeNum) + " SEND TO " + nodeMapping.get(node));
        logger.info(nodeMapping.get(nodeNum) + " REQUEST " +request);
        final ConnectionString connectionString = new ConnectionString(nodeMapping.get(node));
        final HttpClient httpClient = new HttpClient(connectionString);
        try {
            Response invoke = httpClient.invoke(request);
            httpClient.close();
            return invoke;
        } catch (InterruptedException | PoolException | HttpException e) {
            return getResponseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    public static int getNode(final byte[] idArray, int nodeCount) {
        final int hash = Math.abs(Arrays.hashCode(idArray));

        return hash % nodeCount;
    }

    public static void sendErrorInternal(final HttpSession session,
                                   final IOException e) {
        try {
            logger.error(e.getMessage(), e);
            session.sendError("500", e.getMessage());
        } catch (IOException exception) {
            logger.error(e.getMessage(), e);
        }
    }

}
