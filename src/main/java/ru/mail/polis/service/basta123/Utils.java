package ru.mail.polis.service.basta123;

import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Byte.MIN_VALUE;


public final class Utils {

    private Utils() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AckFrom.class);

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

    public static byte[] bufToArray(final ByteBuffer buf) {
        final byte[] byteArray = readArrayBytes(buf);

        for (int x = 0; x < byteArray.length; x++) {
            byteArray[x] -= MIN_VALUE;
        }
        return byteArray;
    }

    public static ByteBuffer arrayToBuf(final byte[] byteArray) {
        final byte[] dupArray = byteArray.clone();
        for (int x = 0; x < byteArray.length; x++) {
            dupArray[x] += MIN_VALUE;
        }
        return ByteBuffer.wrap(dupArray);
    }

    public static byte[] readArrayBytes(final ByteBuffer buf) {
        final ByteBuffer dupBuffer = buf.duplicate();
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

    public static Pair<Integer, Integer> ParseReplicas(final String values)
    {
        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));

        int ack = Integer.valueOf(delimitValues.get(0));
        int from = Integer.valueOf(delimitValues.get(1));

        if ((ack < 1 || from < 1) || (ack > from)) {
            LOGGER.error("error when getting act/from, conditions are not met");
            throw new IllegalArgumentException();
        }


        return new Pair<>(ack,from);
    }

}
