package ru.mail.polis.service.stakenschneider;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class Replica {
    private final int ack;
    private final int from;

    public Replica(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    @NotNull
    private static Replica of(@NotNull final String value) {
        final List<String> values = Splitter.on('/').splitToList(value);
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
                                      final Replica defaultReplicaFactor,
                                      final int clusterSize) throws IllegalArgumentException {
        final Replica replicaFactor = replicas == null ? defaultReplicaFactor : Replica.of(replicas);
        if (replicaFactor.ack < 1 || replicaFactor.from < replicaFactor.ack || replicaFactor.from > clusterSize) {
            throw new IllegalArgumentException("From is too big");
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
