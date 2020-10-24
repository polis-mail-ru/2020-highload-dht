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
import java.util.stream.Collectors;

public final class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    private Util() {

    }

    @NotNull
    public static Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    public static Pair<Integer, Integer> getAckFrom(final Request request,
                                                    final List<String> replicationDefaults,
                                                    final Map<Integer, String> nodeMapping) {
        final int ack;
        final int from;
        String replicas = request.getParameter("replicas");
        if (replicas == null) {
            final Optional<String[]> ackFrom = replicationDefaults.stream()
                    .map(replic -> replic.split("/"))
                    .filter(strings -> Integer.parseInt(strings[1]) == nodeMapping.size())
                    .findFirst();

            final String[] ackFromReal = ackFrom.orElseGet(() -> new String[]{"4", String.valueOf(nodeMapping.size())});
            ack = Integer.parseInt(ackFromReal[0]);
            from = Integer.parseInt(ackFromReal[1]);
        } else {
            replicas = replicas.substring(1);
            ack = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 0));
            from = Integer.parseInt(Iterables.get(Splitter.on('/').split(replicas), 1));
        }

        return new Pair<>(ack, from);
    }

    public static Response routeRequest(final Request request,
                                        final int node,
                                        final Map<Integer, String> nodeMapping)
            throws IOException {

        final ConnectionString connectionString = new ConnectionString(nodeMapping.get(node));
        final HttpClient httpClient = new HttpClient(connectionString);
        try {
            final Response invoke = httpClient.invoke(request);
            httpClient.close();
            return invoke;
        } catch (InterruptedException | PoolException | HttpException e) {
            return getResponseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    public static int getNode(final byte[] idArray, final int nodeCount) {
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

    public static byte[] addTimestamp(final byte[] body) {
        final byte[] timestamp = getTimestampInternal();
        final byte[] newBody = new byte[body.length + timestamp.length];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(timestamp, 0, newBody, body.length, timestamp.length);
        return newBody;
    }

    @NotNull
    public static byte[] getTimestampInternal() {
        final String nanos = String.valueOf(getNanosSync());
        final int[] ints = nanos.chars().toArray();
        final byte[] timestamp = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            timestamp[i] = (byte) ints[i];
        }
        return timestamp;
    }

    public static Pair<byte[], byte[]> getTimestamp(final byte[] body) {
        final int length = String.valueOf(getNanosSync()).length();
        final byte[] timestamp = new byte[length];
        final int realBodyLength = body.length - length;
        System.arraycopy(body, realBodyLength, timestamp, 0, timestamp.length);
        final byte[] newBody = new byte[realBodyLength];
        System.arraycopy(body, 0, newBody, 0, newBody.length);
        return new Pair<>(newBody, timestamp);
    }

    public static Response addTimestampHeader(final byte[] timestamp, final Response response) {
        final String timestampHeader = "Time: ";
        final Byte[] time = new Byte[timestamp.length];
        for (int i = 0; i < time.length; i++) {
            time[i] = timestamp[i];
        }
        final String nanoTime = Arrays.stream(time)
                .mapToInt(value -> value)
                .mapToObj(value -> (char) value)
                .map(String::valueOf)
                .collect(Collectors.joining());

        response.addHeader(timestampHeader + nanoTime);
        return response;
    }

    private static synchronized long getNanosSync() {
        return System.nanoTime();
    }
}
