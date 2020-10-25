package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public interface Topology<N> {

    N getNode(@NotNull final ByteBuffer key);

    boolean isLocal(final String node);

    int size();

    ArrayList<String> getAllNodes();

}
