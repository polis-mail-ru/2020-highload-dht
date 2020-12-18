package ru.mail.polis.service.basta123;

import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AckFrom.class);

    private Utils() {
    }

    /**
     * selects one from all nodes depending on the timestamp.
     *
     * @param valuesFromNodes - List of values.
     * @return valuesFromNodes instance.
     */
    public static TimestampValue valuesSync(final List<TimestampValue> valuesFromNodes) {
        if (valuesFromNodes.size() == 1) {
            return valuesFromNodes.get(0);
        } else {
            return valuesFromNodes.stream()
                    .filter(timestampValue -> !timestampValue.valueExists())
                    .max(Comparator.comparingLong(TimestampValue::getTimeStamp))
                    .orElseGet(TimestampValue::getTimestampValue);
        }
    }

    public static ByteBuffer getByteBufferFromByteArray(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    /**
     * ByteArrayFromByteBuffer.
     *
     * @param buffer input buffer
     * @return bytes array
     */
    public static byte[] getByteArrayFromByteBuffer(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    /**
     * convert buffer to array.
     *
     * @param byteBuffer - ByteBuffer
     * @return byte array
     */
    public static byte[] bufToArray(final ByteBuffer byteBuffer) {
        final byte[] byteArray = readArrayBytes(byteBuffer);

        for (int x = 0; x < byteArray.length; x++) {
            byteArray[x] -= Byte.MIN_VALUE;
        }
        return byteArray;
    }

    /**
     * turn array byte to byte buffer.
     *
     * @param byteArray - byte array
     * @return ByteBuffer
     */
    public static ByteBuffer arrayToBuf(final byte[] byteArray) {
        final byte[] dupArray = byteArray.clone();
        for (int x = 0; x < byteArray.length; x++) {
            dupArray[x] += Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(dupArray);
    }

    /**
     * converts ByteBuffer to byte array.
     *
     * @param byteBuffer - ByteBuffer object
     * @return byte array
     */
    public static byte[] readArrayBytes(final ByteBuffer byteBuffer) {
        final ByteBuffer dupBuffer = byteBuffer.duplicate();
        final byte[] vals = new byte[dupBuffer.remaining()];
        dupBuffer.get(vals);
        return vals;
    }

    /**
     * config server setup.
     *
     * @param port server port
     * @return serverConfig
     */
    public static HttpServerConfig getHttpServerConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        httpServerConfig.selectors = Runtime.getRuntime().availableProcessors();
        return httpServerConfig;
    }

    /**
     * parse request and return ack/from.
     *
     * @param values - request
     * @return pair of ack and from
     */
    public static Pair<Integer, Integer> parseReplicas(final String values) {
        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));

        final int ack = Integer.valueOf(delimitValues.get(0));
        final int from = Integer.valueOf(delimitValues.get(1));

        if ((ack < 1 || from < 1) || (ack > from)) {
            LOGGER.error("error when getting act/from, conditions are not met");
            throw new IllegalArgumentException();
        }

        return new Pair<>(ack, from);
    }

    static boolean isIdValid(final String id, @NotNull final HttpSession httpSession) {
        if (id.isEmpty()) {
            HelperReplicHttpServerImpl.sendResponse(httpSession, Response.BAD_REQUEST);
            return false;
        } else {
            return true;
        }
    }

}
