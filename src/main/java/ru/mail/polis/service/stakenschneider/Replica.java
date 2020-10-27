package ru.mail.polis.service.stakenschneider;

import com.google.common.base.Splitter;
import one.nio.http.HttpSession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static one.nio.http.Response.BAD_REQUEST;

public class Replica {
    private final int ack;
    private final int from;

    public Replica(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    @NotNull
    private static Replica of(@NotNull final String value) {
        final String rem = value.replace("=", "");
        final List<String> values = Splitter.on('/').splitToList(rem);
        if (values.size() != 2) {
            throw new IllegalArgumentException("Wrong replica factor:" + value);
        }
        return new Replica(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)));
    }

    /**
     * Calculate the ReplicaFactor value.
     *
     * @return ReplicaFactor value
     */
    public static Replica calculateRF(final String replicas,
                                      @NotNull final HttpSession session,
                                      final Replica defaultReplicaFactor,
                                      final int clusterSize) throws IOException {
        Replica replicaFactor = null;
        try {
            replicaFactor = replicas == null ? defaultReplicaFactor : Replica.of(replicas);
            if (replicaFactor.ack < 1 || replicaFactor.from < replicaFactor.ack || replicaFactor.from > clusterSize) {
                throw new IllegalArgumentException("From is too big");
            }
            return replicaFactor;
        } catch (IllegalArgumentException e) {
            session.sendError(BAD_REQUEST, "Wrong ReplicaFactor");
        }
        return replicaFactor;
    }

    public int getFrom() {
        return from;
    }

    public int getAck() {
        return ack;
    }
}
