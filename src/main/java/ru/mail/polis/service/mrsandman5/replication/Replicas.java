package ru.mail.polis.service.mrsandman5.replication;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Replicas {

    private final int ack;
    private final int from;

    private Replicas(final int ack,
                    final int from) {
        if (ack > from || ack < 1) {
            throw new IllegalArgumentException("Wrong replicas arguments");
        }
        this.ack = ack;
        this.from = from;
    }

    @NotNull
    public static Replicas quorum(final int nodes) {
        return new Replicas(nodes / 2 + 1, nodes);
    }

    @NotNull
    @SuppressWarnings("UnstableApiUsage")
    public static Replicas parser(@NotNull final String replicas) {
        final List<String> params = Splitter.on('/').splitToList(replicas);
        return new Replicas(Integer.parseInt(params.get(0)), Integer.parseInt(params.get(1)));
    }

    public int getFrom() {
        return from;
    }

    public int getAck() {
        return ack;
    }
}
