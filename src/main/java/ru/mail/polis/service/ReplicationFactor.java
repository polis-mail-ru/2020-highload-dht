package ru.mail.polis.service;

import java.util.Arrays;
import java.util.List;

final class ReplicationFactor {

    private static final String RF_ERROR = "Invalid replication factor";

    private final int ack;
    private final int from;

    private ReplicationFactor(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    static ReplicationFactor getQuorum(final int topologySize) {
        return new ReplicationFactor(topologySize / 2 + 1, topologySize);
    }

    static ReplicationFactor createReplicationFactor(
            final String values
    ) throws IllegalArgumentException {
        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));

        final int ack = Integer.parseInt(delimitValues.get(0));
        final int from = Integer.parseInt(delimitValues.get(1));
        final boolean negativeValuesPresent = ack < 1 || from < 1;

        if (delimitValues.size() != 2 || negativeValuesPresent || ack > from) {
            throw new IllegalArgumentException(RF_ERROR);
        }

        return new ReplicationFactor(ack, from);
    }

    int getAck() {
        return ack;
    }

    int getFrom() {
        return from;
    }
}
