package ru.mail.polis.service.kate.moreva;

import com.google.common.base.Splitter;

import java.util.List;

public class Replicas {
    private final int ack;
    private final int from;

    private Replicas(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    public static Replicas quorum(final int count) {
        int n = count / 2 + 1;
        return new Replicas(n, count);
    }

    public static Replicas parser(final String replicas) {
        final List<String> params = Splitter.on('/').splitToList(replicas);
        if (params.size() != 2) {
            throw new IllegalArgumentException("Wrong Replica factor: " + replicas);
        }
        final int confirmation = Integer.parseInt(params.get(0));
        final int from = Integer.parseInt(params.get(1));
        return new Replicas(confirmation, from);
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
