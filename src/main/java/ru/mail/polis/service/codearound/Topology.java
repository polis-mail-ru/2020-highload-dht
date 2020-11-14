/*package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;

public interface Topology<N> {

    @NotNull
    N primaryFor(@NotNull ByteBuffer nodeKey);

    N[] replicasFor(ByteBuffer key, int numOfReplicas);

    boolean isSelfId(@NotNull N node);

    int getClusterSize();

    Set<N> getNodes();

    N getSelfId();
}*/

package ru.mail.polis.service.codearound;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 *  basis of extended functionality necessary in sharded design of cluster.
 */
@ThreadSafe
public interface Topology<T> {

    @NotNull
    T primaryFor(@NotNull ByteBuffer key);

    boolean isThisNode(@NotNull T nodeId);

    @NotNull
    Set<T> getNodes();

    @NotNull
    String[] replicasFor(@NotNull final ByteBuffer id, int numOfReplicas);

    @NotNull
    String getThisNode();

    int getClusterSize();
}
