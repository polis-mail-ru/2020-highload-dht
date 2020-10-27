package ru.mail.polis.service.nik27090;

import one.nio.http.HttpClient;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface Topology<N> {
    List<Response> getResponseFromNodes(List<String> nodes,
                                        Request request,
                                        Response localResponse,
                                        Map<String, HttpClient> nodeToClient);

    @NotNull
    N[] getReplicas(@NotNull ByteBuffer key, int countReplicas);

    boolean isCurrentNode(@NotNull N node);

    @NotNull
    N[] all();

    AckFrom parseAckFrom(String askFrom);

    boolean isProxyReq(Request request);

    int size();
}
