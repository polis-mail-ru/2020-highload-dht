package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import com.google.common.primitives.Bytes;
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
import ru.mail.polis.service.Mapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    private Util() {

    }

    /**
     * Get response with no Body.
     *
     * @param requestType - type
     * @return - the built request.
     */
    @NotNull
    public static Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    /**
     * Get RF.
     *
     * @param request - request to replicate.
     * @param replicationDefaults - default RFs.
     * @param nodeMapping - node list.
     * @return - pair of ack and from.
     */
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

    /**
     * Hash route request.
     *
     * @param request - request to route.
     * @param node - node to route the request to.
     * @param nodeMapping - node list.
     * @return - returned response.
     * @throws IOException - for errors with the net.
     */
    public static Response routeRequest(final Request request,
                                        final int node,
                                        final Map<Integer, String> nodeMapping)
            throws IOException {

        final ConnectionString connectionString = new ConnectionString(nodeMapping.get(node));
        try (HttpClient httpClient = new HttpClient(connectionString)) {
            return httpClient.invoke(request);
        } catch (InterruptedException | PoolException | HttpException e) {
            return getResponseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    /**
     * Get node with hash.
     *
     * @param idArray - key.
     * @param nodeCount - amount of nodes.
     * @return - the node number.
     */
    public static int getNode(final byte[] idArray, final int nodeCount) {
        int hash = Math.abs(Arrays.hashCode(idArray));
        hash = hash < 0 ? -hash : hash;
        return hash % nodeCount;
    }

    /**
     * Send the 500 status error.
     *
     * @param session - session to use.
     * @param e - the error that casued the 500 error.
     */
    public static void sendErrorInternal(final HttpSession session,
                                   final IOException e) {
        try {
            logger.error(e.getMessage(), e);
            session.sendError("500", e.getMessage());
        } catch (IOException exception) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Add timestamp to body.
     *
     * @param body - body value.
     * @return - body with timestamp.
     */
    public static byte[] addTimestamp(final byte[] body) {
        final byte[] timestamp = getTimestampInternal();
        final byte[] newBody = new byte[body.length + timestamp.length];
        System.arraycopy(body, 0, newBody, 0, body.length);
        System.arraycopy(timestamp, 0, newBody, body.length, timestamp.length);
        return newBody;
    }

    /**
     * Get current timestamp.
     *
     * @return - the timestamp.
     */
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

    /**
     * Separate out the timestamp from body.
     *
     * @param body - body with timestamp.
     * @return - pair of timestamp and pure body.
     */
    public static Pair<byte[], byte[]> getTimestamp(final byte[] body) {
        final int length = String.valueOf(getNanosSync()).length();
        final byte[] timestamp = new byte[length];
        final int realBodyLength = body.length - length;
        System.arraycopy(body, realBodyLength, timestamp, 0, timestamp.length);
        final byte[] newBody = new byte[realBodyLength];
        System.arraycopy(body, 0, newBody, 0, newBody.length);
        return new Pair<>(newBody, timestamp);
    }

    /**
     * Add timestamp to header.
     *
     * @param timestamp - timestamp to add.
     * @param response - the response to which to add the timestamp.
     * @return - the modified response.
     */
    public static Response addTimestampHeader(final byte[] timestamp, final Response response) {
        final String timestampHeader = "Time: ";
        final Integer[] integers = Bytes.asList(timestamp).stream()
                .map(Byte::intValue)
                .toArray(Integer[]::new);
        final String nanoTime = Arrays.stream(integers)
                .mapToInt(value -> value)
                .mapToObj(value -> (char) value)
                .map(String::valueOf)
                .collect(Collectors.joining());
        response.addHeader(timestampHeader + nanoTime);
        return response;
    }

    /**
     * Get byte buffer body.
     *
     * @param request - request from which to get the body.
     * @return - the byte buffer.
     */
    public static ByteBuffer getByteBufferValue(final Request request) {
        byte[] body = request.getBody();
        body = Util.addTimestamp(body);
        return Mapper.fromBytes(body);
    }

    /**
     * Get key from param.
     *
     * @param idParam - key param.
     * @return - key byte buffer.
     */
    @NotNull
    public static ByteBuffer getKey(final String idParam) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        return Mapper.fromBytes(idArray);
    }

    private static synchronized long getNanosSync() {
        return System.nanoTime();
    }
}
