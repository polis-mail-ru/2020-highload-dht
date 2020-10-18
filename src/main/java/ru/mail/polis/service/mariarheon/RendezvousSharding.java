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
    private Set<String> nodes;
    private String currentNode;
    private Map<String, HttpClient> clients;

    public RendezvousSharding(@NotNull final Set<String> topology,
                              @NotNull final String currentNode) {
        this.nodes = topology;
        this.currentNode = currentNode;
        clients = new HashMap<>();
        for (String node : nodes) {
            if (!isMe(node)) {
                clients.put(node, new HttpClient(new ConnectionString(node + "?timeout=10")));
            }
        }
    }

    public String getResponsibleNode(@NotNull final String key) {
        String requiredNode = null;
        int max = Integer.MIN_VALUE;
        for (String node : nodes) {
            int hashCode = Objects.hash(key, node);
            if (hashCode > max) {
                max = hashCode;
                requiredNode = node;
            }
        }
        return requiredNode;
    }

    public boolean isMe(String node) {
        return node.equals(currentNode);
    }

    public Response passOn(String to, Request request) throws InterruptedException, IOException, HttpException, PoolException {
        return clients.get(to).invoke(request, 100000);
    }
}
