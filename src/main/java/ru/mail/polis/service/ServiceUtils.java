package ru.mail.polis.service;

import one.nio.http.HttpServerConfig;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ServiceUtils {

    static final Logger logger = LoggerFactory.getLogger(ServiceUtils.class);

    private ServiceUtils() {
    }

    static void assertNotEmpty(@Nullable final String str) throws IllegalArgumentException {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException("Required param must not be empty!");
        }
    }

    @NotNull
    static HttpServerConfig configFrom(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

    @NotNull
    static ByteBuffer byteBufferFrom(@NotNull final String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    static ByteBuffer byteBufferFrom(@NotNull final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    @NotNull
    static byte[] byteArrayFrom(@NotNull final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
