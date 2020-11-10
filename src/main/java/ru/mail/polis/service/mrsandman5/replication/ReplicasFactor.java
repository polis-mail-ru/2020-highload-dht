package ru.mail.polis.service.mrsandman5.replication;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReplicasFactor {

    private final int ack;
    private final int from;

    private ReplicasFactor(final int ack,
                           final int from) {
        this.ack = ack;
        this.from = from;
    }

    @NotNull
    public static ReplicasFactor quorum(final int nodes) {
        return new ReplicasFactor(nodes / 2 + 1, nodes);
    }

    @NotNull
    @SuppressWarnings("UnstableApiUsage")
    public static ReplicasFactor parser(@NotNull final String replicas) {
        final List<String> params = Splitter.on('/').splitToList(replicas);
        assert params.size() == 2;
        final int ackParam = Integer.parseInt(params.get(0));
        final int fromParam = Integer.parseInt(params.get(1));
        return new ReplicasFactor(ackParam, fromParam);
    }

    public int getFrom() {
        return from;
    }

    public int getAck() {
        return ack;
    }
}
