package ru.mail.polis.service.mrsandman5.replication;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class Replicas {

    private final int ack;
    private final int from;

    public Replicas(final int ack,
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
    public static Replicas parser(@NotNull final String replicas) {
        final var params = replicas.split("/");
        if (params.length != 2) {
            throw new IllegalArgumentException("Wrong replicas: " + Arrays.toString(params));
        }

        return new Replicas(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
