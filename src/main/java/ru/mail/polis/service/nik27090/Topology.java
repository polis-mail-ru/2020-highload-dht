package ru.mail.polis.service.nik27090;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Topology<N> {
    List<Response> getResponseFromNodes(final List<String> nodes,
                                        final Request request,
                                        final CompletableFuture<Response> localResponse,
                                        final HttpClient nodeToClient);

    @NotNull
    N[] getReplicas(@NotNull final ByteBuffer key, final int countReplicas);

    boolean isCurrentNode(@NotNull final N node);

    @NotNull
    N[] all();

    AckFrom parseAckFrom(final String askFrom);

    boolean isProxyReq(final Request request);

    int size();
}
