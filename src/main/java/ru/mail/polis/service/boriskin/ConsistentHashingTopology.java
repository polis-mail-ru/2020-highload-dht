package ru.mail.polis.service.boriskin;

import org.jetbrains.annotations.NotNull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ConsistentHashingTopology extends Topology<String> {

    boolean add(@NotNull final String node);

    boolean remove(@NotNull final String node);
}
