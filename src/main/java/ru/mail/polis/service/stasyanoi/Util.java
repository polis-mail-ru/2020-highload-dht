package ru.mail.polis.service.stasyanoi;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import com.google.common.primitives.Bytes;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.Mapper;
import ru.mail.polis.service.stasyanoi.server.helpers.AckFrom;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Util {

    private final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Get response with no Body.
     *
     * @param requestType - type
     * @return - the built request.
     */
    @NotNull
    public Response responseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    /**
     * Get node with hash.
     *
     * @param nodeCount - amount of nodes.
     * @return - the node number.
     */
    public int getNode(final String idParam, final int nodeCount) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        final int hash = Math.abs(Arrays.hashCode(idArray));
        final int absoluteHash = hash < 0 ? -hash : hash;
        return absoluteHash % nodeCount;
    }

    /**
     * Filter responses for throwables.
     *
     * @param response  - response received.
     * @param throwable - throwable that has been thrown.
     * @return the response after filtration.
     */
    public Response filterResponse(final Response response, final Throwable throwable) {
        if (throwable == null) {
            return response;
        } else {
            return responseWithNoBody(Response.INTERNAL_ERROR);
        }
    }

    /**
     * Add timestamp to body.
     *
     * @param body - body value.
     * @return - body with timestamp.
     */
    public byte[] addTimestamp(final byte[] body) {
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
    public synchronized byte[] getTimestampInternal() {
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
    public Pair<byte[], byte[]> getTimestamp(final byte[] body) {
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
     * @param response  - the response to which to add the timestamp.
     * @return - the modified response.
     */
    public Response addTimestampHeader(final byte[] timestamp, final Response response) {
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
    public ByteBuffer getByteBufferValue(final Request request) {
        byte[] body = request.getBody();
        body = addTimestamp(body);
        return Mapper.fromBytes(body);
    }

    /**
     * Get key from param.
     *
     * @param idParam - key param.
     * @return - key byte buffer.
     */
    @NotNull
    public ByteBuffer getKey(final String idParam) {
        final byte[] idArray = idParam.getBytes(StandardCharsets.UTF_8);
        return Mapper.fromBytes(idArray);
    }

    private synchronized long getNanosSync() {
        return System.nanoTime();
    }

    /**
     * Send 503 error.
     *
     * @param errorSession - session to which to send the error.
     */
    public void send503Error(final HttpSession errorSession) {
        try {
            errorSession.sendResponse(responseWithNoBody(Response.SERVICE_UNAVAILABLE));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Get one nio response.
     *
     * @param javaResponse - response.
     * @return one nio response.
     */
    public Response getOneNioResponse(final HttpResponse<byte[]> javaResponse) {
        final Response response = new Response(String.valueOf(javaResponse.statusCode()), javaResponse.body());
        javaResponse.headers().map().forEach((s, strings) -> response.addHeader(s + ": " + strings.get(0)));
        return response;
    }

    /**
     * Get net java request.
     *
     * @param oneNioRequest - one nio request.
     * @param host - host.
     * @return - java net request.
     */
    public HttpRequest getJavaRequest(final Request oneNioRequest, final String host) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        final String newPath = oneNioRequest.getPath() + "?" + oneNioRequest.getQueryString();
        final String uri = host + newPath;
        final String methodName = oneNioRequest.getMethodName();
        final HttpRequest.Builder requestBuilder = builder
                .timeout(Duration.ofSeconds(1))
                .uri(URI.create(uri));
        if ("GET".equalsIgnoreCase(methodName)) {
            return requestBuilder.GET().build();
        } else if ("PUT".equalsIgnoreCase(methodName)) {
            final HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(oneNioRequest
                    .getBody());
            return requestBuilder.PUT(bodyPublisher).build();
        } else {
            return requestBuilder.DELETE().build();
        }
    }

    /**
     * Get request with replication path.
     *
     * @param request - input request.
     * @param port - post for the current server.
     * @return new request with replication path.
     */
    public Request getNewReplicationRequest(final Request request, final int port) {
        final String path = request.getPath();
        final String queryString = request.getQueryString();
        final String newPath = path + "/rep?" + queryString;
        final Request requestNew = getCloneRequest(request, newPath, port);
        requestNew.setBody(request.getBody());
        return requestNew;
    }

    /**
     * Get request with no replication header.
     *
     * @param request - the request to which to add the header.
     * @param port - port of the current server.
     * @return - new no replication request
     */
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

    /**
     * Get clone of input request.
     *
     * @param request - input request.
     * @param newPath - new path to which to send.
     * @param thisServerPort - the port of the sender server.
     * @return - cloned request.
     */
    public Request getCloneRequest(final Request request, final String newPath, final int thisServerPort) {
        final Request noRepRequest = new Request(request.getMethod(), newPath, true);
        Arrays.stream(request.getHeaders())
                .filter(Objects::nonNull)
                .filter(header -> !header.contains("Host: "))
                .forEach(noRepRequest::addHeader);
        noRepRequest.addHeader("Host: localhost:" + thisServerPort);
        return noRepRequest;
    }

    /**
     * Get replication factor object.
     *
     * @param replicas - replicas header.
     * @param size - size of cluster.
     * @return AckFrom object.
     */
    public AckFrom getRF(final String replicas, final int size) {
        final int ack;
        final int from;
        if (replicas == null) {
            ack = size / 2 + 1;
            from = size;
        } else {
            final String pureReplicasHeader = replicas.substring(1);
            ack = Integer.parseInt(Iterables.get(Splitter.on('/').split(pureReplicasHeader), 0));
            from = Integer.parseInt(Iterables.get(Splitter.on('/').split(pureReplicasHeader), 1));
        }
        return new AckFrom(ack, from);
    }
}
