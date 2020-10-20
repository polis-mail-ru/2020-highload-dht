package ru.mail.polis.service.alexander.marashov.topologies;

import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Topology<N> {
    boolean isLocal(final N node);
    N primaryFor(final ByteBuffer key);
    Iterator<N> iterator();
    int size();
}
