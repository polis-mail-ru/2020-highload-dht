package ru.mail.polis.service.alexander.marashov.topologies;

import java.nio.ByteBuffer;

public interface Topology<N> {
    boolean isLocal(final N node);

    N primaryFor(final ByteBuffer key);

    String[] all();

    int size();
}
