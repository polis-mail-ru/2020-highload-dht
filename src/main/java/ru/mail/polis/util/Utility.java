package ru.mail.polis.util;

import one.nio.http.HttpClient;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Utility {
    public static final String DEADFLAG_TIMESTAMP_HEADER = "XDeadFlagTimestamp";
    public static final String TIME_HEADER = "XTime";

    private Utility() {
    }

    public static boolean validateId(@NotNull final String id) {
        return !id.isEmpty();
    }

    public static ByteBuffer byteBufferFromString(@NotNull final String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks is homeNode in nodeReplicas.
     *
     * @param homeNode     homeNode
     * @param nodeReplicas array of replicas
     * @return is home in replicas
     */
    public static boolean isHomeInReplicas(@NotNull final String homeNode,
                                           @NotNull final String... nodeReplicas) {
        for (final var node :
                nodeReplicas) {
            if (node.equals(homeNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configuring http server from port.
     *
     * @param port http server's port
     * @return HttpServerConfig
     */
    @NotNull
    public static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[1];
        config.acceptors[0] = ac;
        return config;
    }

    /**
     * Make map String to HttpClient from nodes list.
     *
     * @param homeNode node ignored in nodes
     * @param nodes    list of nodes
     * @return map string to httpclient
     */
    public static Map<String, HttpClient> urltoClientFromSet(@NotNull final String homeNode,
                                                             @NotNull final String... nodes) {
        final Map<String, HttpClient> result = new HashMap<>();
        for (final var url : nodes) {
            if (url.equals(homeNode)) {
                continue;
            }
            if (result.put(url, new HttpClient(new ConnectionString(url))) != null) {
                throw new RuntimeException("Duplicated url in nodes.");
            }
        }
        return result;
    }

    /**
     * Send response with catching IOException and rethrowing it as RuntimeException.
     *
     * @param session  session for sending response
     * @param response response that sended
     */
    public static void sendResponse(@NotNull final HttpSession session,
                                    @NotNull final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("IOException in sending response", e);
        }
    }

    /**
     * Make byte array from ByteBuffer.
     *
     * @param b ByteBuffer
     * @return byte array
     */
    @NotNull
    public static byte[] fromByteBuffer(@NotNull final ByteBuffer b) {
        final byte[] out = new byte[b.remaining()];
        b.get(out);
        return out;
    }
}
