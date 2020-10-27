package ru.mail.polis.service.manikhin;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Replicas {
    private final int ack;
    private final int from;

    public Replicas(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    public static Replicas quorum(final int count) {
        final int n = count / 2 + 1;
        return new Replicas(n, count);
    }

    /**
     * Parses the request to get the needed number of answers (ack) and nodes (from).
     * */
    public static Replicas parser(final String replicas) {
        final List<String> params = Arrays.asList(replicas.replace("=", "").split("/"));
        final int confirmation, from;

        if (params.size() != 2) {
            throw new IllegalArgumentException("Wrong Replica factor: " + replicas);
        }

        confirmation = Integer.parseInt(params.get(0));
        from = Integer.parseInt(params.get(1));

        return new Replicas(confirmation, from);
    }

    public static Replicas ReplicaFactor(final String replicas,
                                         @NotNull final HttpSession session,
                                         final Replicas defaultReplicaFactor,
                                         final int clusterSize) throws IOException {
        Replicas replicaFactor = null;

        try {
            replicaFactor = replicas == null ? defaultReplicaFactor : parser(replicas);
            if (replicaFactor.ack < 1 || replicaFactor.from < replicaFactor.ack || replicaFactor.from > clusterSize) {
                throw new IllegalArgumentException("From is is very big");
            }
            return replicaFactor;
        } catch (IllegalArgumentException error) {
            session.sendError(Response.BAD_REQUEST, "Wrong ReplicaFactor");
        }

        return replicaFactor;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}