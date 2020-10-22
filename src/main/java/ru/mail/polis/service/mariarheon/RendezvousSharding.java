package ru.mail.polis.service.mariarheon;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RendezvousSharding {
    private final Set<String> nodes;
    private final String currentNode;
    private final Map<String, HttpClient> clients;
    private static final String TIMEOUT_SUFFIX = "?timeout=100";

    /**
     * Constructor for RendezvousSharding.
     *
     * @param topology - topology.
     * @param currentNode - current node.
     */
    public RendezvousSharding(@NotNull final Set<String> topology,
                              @NotNull final String currentNode) {
        this.nodes = topology;
        this.currentNode = currentNode;
        clients = new HashMap<>();
        for (final String node : nodes) {
            if (!node.equals(currentNode)) {
                clients.put(node, new HttpClient(new ConnectionString(node + TIMEOUT_SUFFIX)));
            }
        }
    }

    /**
     * get responsible node.
     * @param key - key.
     * @return reuired node.
     */
    public String getResponsibleNode(@NotNull final String key) {
        String requiredNode = null;
        int max = Integer.MIN_VALUE;
        for (final String node : nodes) {
            final int hashCode = Objects.hash(key, node);
            if (hashCode > max) {
                max = hashCode;
                requiredNode = node;
            }
        }
        return requiredNode;
    }

    public boolean isMe(final String node) {
        return node.equals(currentNode);
    }

    /**
     * Pass request to another node.
     * @param to node where request should be sent.
     * @param request the request.
     * @return response from node. 
     */
    public Response passOn(final String to, final Request request) throws InterruptedException,
                                                              IOException,
                                                              HttpException,
                                                              PoolException {
        return clients.get(to).invoke(request);
    }
}
