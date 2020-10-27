package ru.mail.polis.util;

import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Util {
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
        for (int i=keys.length - replicasNumber; i<keys.length; i++) {
            resultNodes.add(hash.get(keys[i]));
        }
        return resultNodes;
    }
}
