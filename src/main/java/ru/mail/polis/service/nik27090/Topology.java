package ru.mail.polis.service.nik27090;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {
    @NotNull
    N[] getReplicas(@NotNull ByteBuffer key, int countReplicas);

    boolean isCurrentNode(@NotNull N node);

    @NotNull
    N[] all();

    AckFrom parseAckFrom(String askFrom);

    boolean isProxyReq(Request request);

    int size();
}
