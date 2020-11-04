package ru.mail.polis.service;

import java.util.Arrays;
import java.util.List;

class ReplicationFactor {

    private static final String RF_ERROR = "Invalid replication factor";

    private final int ack;
    private final int from;

    ReplicationFactor(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    private static ReplicationFactor createReplicationFactor(
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

    static ReplicationFactor getReplicationFactor(
            final String nodeReplicas,
            final ReplicationFactor replicationFactor
    ) throws IllegalArgumentException {
        return nodeReplicas == null ? replicationFactor : ReplicationFactor.createReplicationFactor(
                nodeReplicas
        );
    }

    int getAck() {
        return ack;
    }

    int getFrom() {
        return from;
    }
}
