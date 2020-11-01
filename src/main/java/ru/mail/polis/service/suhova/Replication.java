package ru.mail.polis.service.suhova;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Replication {
    private static final Logger logger = LoggerFactory.getLogger(Replication.class);
    private final int acks;
    private final int replicas;

    public Replication(final int acks, final int replicas) {
        this.acks = acks;
        this.replicas = replicas;
    }

    public int getAcks() {
        return acks;
    }

    public int getReplicas() {
        return replicas;
    }

    /**
     * Create Replication.
     *
     * @param replicationSpec replica
     * @return new Replication
     */
    @NotNull
    public static Replication of(@NotNull final String replicationSpec) {
        final List<String> values = Splitter.on('/').splitToList(replicationSpec);
        if (values.size() != 2) {
            throw new IllegalArgumentException("Incorrect replica parameter");
        }
        try {
            return new Replication(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)));
        } catch (NumberFormatException e) {
            logger.error("Parameter 'replicas' cannot be parsed! {}", replicationSpec, e);
            throw new IllegalArgumentException(e);
        }
    }
}
