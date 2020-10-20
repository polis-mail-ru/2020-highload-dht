package ru.mail.polis.service;


import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface Topology<N> {

    N getNode(@NotNull final ByteBuffer key);

    boolean isLocal(final String node);

    int size();

    N[] getAllNodes();

}
