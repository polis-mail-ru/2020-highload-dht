/*package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;

public interface Topology<N> {

    @NotNull
    N getNodeForKey(@NotNull ByteBuffer nodeKey);

    N[] getNodesForKey(ByteBuffer key, int numOfReplicas);

    boolean isSelfId(@NotNull N node);

    int getSize();

    Set<N> getAllNodes();

    N getSelfId();
}*/

package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.List;

@ThreadSafe
public interface Topology<T> {

    @NotNull
    T getNodeForKey(@NotNull final ByteBuffer key);

    @NotNull
    List<String> getAllNodes();

    boolean isLocal(final String node);

    @NotNull
    List<String> getNodesForKey(@NotNull final ByteBuffer id, final int numOfReplicas);

    int getSize();

    String getLocalNode();
}
