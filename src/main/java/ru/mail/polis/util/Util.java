package ru.mail.polis.util;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public final class Util {
    public static final String PROXY_HEADER = "X-OK-Proxy";

    private Util() {
        /* Add private constructor to prevent instantiation */
    }

    /**
     * This converts ByteBuffer to byte array.
     *
     * @param buffer ByteBuffer
     * @return array bytes
     */
    public static byte[] toByteArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);
        return arr;
    }

    /**
     * This wraps byte array into ByteBuffer.
     *
     * @param arr byte array
     * @return ByteBuffer with shifted bytes
     */
    public static ByteBuffer fromShiftedArray(@NotNull final byte[] arr) {
        final byte[] copy = Arrays.copyOf(arr, arr.length);

        int i = 0;
        while (i < copy.length) {
            copy[i] = (byte) (Byte.toUnsignedInt(copy[i]) + Byte.MIN_VALUE);
            i++;
        }

        return ByteBuffer.wrap(copy);
    }

    /**
     * This takes array from a ByteBuffer and performs all bytes shift by MIN_VALUE.
     *
     * @param buffer ByteBuffer
     * @return array with shifted bytes
     */
    public static byte[] toShiftedArray(@NotNull final ByteBuffer buffer) {
        final ByteBuffer copy = buffer.duplicate();
        final byte[] arr = new byte[copy.remaining()];
        copy.get(arr);

        int i = 0;
        while (i < arr.length) {
            arr[i] = (byte) (Byte.toUnsignedInt(arr[i]) - Byte.MIN_VALUE);
            i++;
        }

        return arr;
    }

    /**
     * Wraps string to ByteBuffer.
     *
     * @param id - string to wrap
     * @return ByteBuffer
     */
    public static ByteBuffer toByteBuffer(final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Redirects request to target node.
     *
     * @param node - target node id
     * @param req  - one-nio request
     * @return HttpRequest.Builder
     */
    public static HttpRequest.Builder setProxyHeader(final String node, @NotNull final Request req) {
        return HttpRequest.newBuilder()
                .uri(URI.create(node + req.getURI()))
                .timeout(Duration.ofSeconds(1))
                .setHeader(PROXY_HEADER, "True");
    }

    /**
     * This returns CompletableFuture collection of generic entities when essential num of ack is received.
     *
     * @param futures  - generic futures collections
     * @param ack      - minimum number of successes to complete future
     * @param executor - thread pool executor
     * @param <T>      - generic future type
     * @return - CompletableFuture collection of generic entities
     */
    public static <T> CompletableFuture<Collection<T>> getMinimumAckData(
            @NotNull final Collection<CompletableFuture<T>> futures,
            final int ack,
            @NotNull final ExecutorService executor) {
        final AtomicInteger oksLeft = new AtomicInteger(ack);
        final AtomicInteger errorsLeft = new AtomicInteger(futures.size() - ack + 1);
        final Collection<T> results = new CopyOnWriteArrayList<>();
        final CompletableFuture<Collection<T>> target = new CompletableFuture<>();

        if (futures.size() < ack) {
            target.completeExceptionally(new IllegalStateException("Futures's size is less than ack"));
            return target;
        }

        futures.forEach(f -> f.whenCompleteAsync((v, t) -> {
            if (t == null) {
                results.add(v);
                if (oksLeft.decrementAndGet() == 0) {
                    target.complete(results);
                }
            } else {
                if (errorsLeft.decrementAndGet() == 0) {
                    target.completeExceptionally(new IllegalStateException("Can't get " + ack + " values"));
                }
            }
        }, executor).isCancelled());
        return target;
    }
}
