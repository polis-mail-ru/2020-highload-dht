package ru.mail.polis.util;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    private static final String INTERNAL_ERROR = "Internal Server Error";
    private static final String RESPONSE_ERROR = "Can not send response.";

    private Util() {

    }

    /**
     * Convert data from ByteBuffer to byte[].
     *
     * @param buffer data to convert
     * @return byte array
     */
    public static byte[] byteBufferToBytes(@NotNull final ByteBuffer buffer) {
        final ByteBuffer bufCopy = buffer.duplicate();
        final byte[] bytes = new byte[bufCopy.remaining()];
        bufCopy.get(bytes,0,bytes.length);
        bufCopy.clear();
        return bytes;
    }

    /**
     * Convert data from ByteBuffer to unsigned byte[].
     *
     * @param buffer data to convert
     * @return unsigned byte array
     */
    public static byte[] pack(@NotNull final ByteBuffer buffer) {
        byte[] bytes = byteBufferToBytes(buffer);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= Byte.MIN_VALUE;
        }
        return bytes;
    }

    /**
     * Convert data from unsigned byte[] to signed ByteBuffer.
     *
     * @param bytes data to convert
     * @return signed ByteBuffer
     */
    public static ByteBuffer unpack(@NotNull final byte[] bytes) {
        byte[] bytesCopy = bytes.clone();
        for (int i = 0; i < bytesCopy.length; i++) {
            bytesCopy[i] ^= Byte.MIN_VALUE;
        }
        return ByteBuffer.wrap(bytesCopy);
    }

    /**
     * Returns nodes that stores data for a given key by rendezvous hashing algorithm.
     *
     * @param nodes - list of existing nodes
     * @param key - data id
     * @param replicasNumber - number of nodes to store data
     */
    public static Set<String> getNodes(final Set<String> nodes,
                                       final String key,
                                       final int replicasNumber) {
        final Map<Integer,String> hash = new HashMap<>();
        final Set<String> resultNodes = new HashSet<>();
        for (final String node : nodes) {
            hash.put(Hash.murmur3(node + key), node);
        }
        final Object[] keys = hash.keySet().toArray();
        Arrays.sort(keys);
        for (int i = keys.length - replicasNumber; i < keys.length; i++) {
            resultNodes.add(hash.get(keys[i]));
        }
        return resultNodes;
    }

    /**
     * Send given response.
     */
    public static void sendResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException ex) {
            log.error(RESPONSE_ERROR, ex);
        }
    }

    /**
     * Send given response when future completes.
     */
    public static void sendResponseFromFuture(final HttpSession session,
                                       final CompletableFuture<Response> response) {
        if (response.whenComplete((r,t) -> {
            if (t == null) {
                try {
                    session.sendResponse(r);
                } catch (IOException ex) {
                    log.error(RESPONSE_ERROR, ex);
                }
            } else {
                try {
                    session.sendError(INTERNAL_ERROR, t.getMessage());
                } catch (IOException ex) {
                    log.error("Can not send error.", ex);
                }

            }
        }).isCancelled()) {
            log.error(RESPONSE_ERROR);
        }
    }
}
