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
    public static final String DEADFLAG_TIMESTAMP_HEADER = "DeadFlagTimestamp";
    public static final String TIME_HEADER = "Time";
    
    private Utility() {
    }
    
    public static boolean invalid(@NotNull final String id) {
        return id.isEmpty();
    }

    public static long fromByteArray(final byte[] in, final int offset, final int size) {
        if (size != Long.BYTES) {
            throw new IllegalArgumentException("Byte arrays is not the same size as Long");
        }
        return ByteBuffer.wrap(in, offset, size).getLong();
    }

    public static long fromByteArray(final byte[] in) {
        return fromByteArray(in, 0, in.length);
    }

    public static ByteBuffer byteBufferFromString(@NotNull final String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Checks is el in elements' array.
     *
     * @param el     el
     * @param elements array
     * @return is el in elements' array
     */
    @SafeVarargs
    public static <T> boolean arrayContains(@NotNull final T el,
                                            @NotNull final T... elements) {
        for (final var node :
                elements) {
            if (node.equals(el)) {
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
