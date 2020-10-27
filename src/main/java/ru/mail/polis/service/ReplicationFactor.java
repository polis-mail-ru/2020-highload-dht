package ru.mail.polis.service;

import one.nio.http.HttpSession;
import one.nio.http.Response;

import java.io.IOException;
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

    private static ReplicationFactor createReplicationFactor(final String values, final HttpSession session) throws IOException {

        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));

        if (delimitValues.size() != 2) {
            session.sendError(Response.BAD_REQUEST, RF_ERROR);
        }
        if (Integer.parseInt(delimitValues.get(0)) < 1 || Integer.parseInt(delimitValues.get(1)) < 1) {
            session.sendError(Response.BAD_REQUEST, RF_ERROR);
        }
        if (Integer.parseInt(delimitValues.get(0)) > Integer.parseInt(delimitValues.get(1))) {
            session.sendError(Response.BAD_REQUEST, RF_ERROR);
        }

        final int ack = Integer.parseInt(delimitValues.get(0));
        final int from = Integer.parseInt(delimitValues.get(1));

        return new ReplicationFactor(ack, from);
    }

    static ReplicationFactor getReplicationFactor(
            final String nodeReplicas,
            final ReplicationFactor replicationFactor,
            final HttpSession session
    ) throws IOException {
        return nodeReplicas == null ? replicationFactor : ReplicationFactor.createReplicationFactor(nodeReplicas, session);
    }

    int getAck() {
        return ack;
    }

    int getFrom() {
        return from;
    }
}
