package ru.mail.polis.service.mrsandman5.clustering;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Implemented by example - https://github.com/Jaskey/ConsistentHash.
 * */
public final class ConsistentHashingTopology<T> implements Topology<T> {

    private static final String BETA = "UnstableApiUsage";
    private final T me;
    @NotNull
    private final Set<T> topology;
    private final NavigableMap<Long, VirtualNode<T>> ring = new TreeMap<>();
    @SuppressWarnings(BETA)
    private final HashFunction hashFunction = Hashing.murmur3_128();

    /** Consistent hashing cluster topology.
     * @param topology - nodes of the cluster
     * @param me       - name of the current node
     * @param virtualNodeCount - virtual nodes for hash ring
     */
    public ConsistentHashingTopology(@NotNull final Set<T> topology,
                                     @NotNull final T me,
                                     final int virtualNodeCount) {
        this.me = me;
        this.topology = topology;
        this.topology.forEach(node -> addNode(node, virtualNodeCount));
    }

    @SuppressWarnings(BETA)
    @NotNull
    @Override
    public T primaryFor(@NotNull final ByteBuffer key) {
        final long hash = hashFunction.hashBytes(key.duplicate()).asLong();
        final var nodeEntry = ring.ceilingEntry(hash);
        return nodeEntry == null
                ? ring.firstEntry().getValue().physicalNode
                : nodeEntry.getValue().physicalNode;
    }

    @SuppressWarnings(BETA)
    @NotNull
    @Override
    public Set<T> replicasFor(@NotNull final ByteBuffer key,
                              @NotNull final ReplicasFactor replicasFactor) {
        if (replicasFactor.getFrom() > topology.size()) {
            throw new IllegalArgumentException("Number of nodes is bigger than from!");
        }
        final long hash = hashFunction.hashBytes(key.duplicate()).asLong();
        final Set<T> result = new HashSet<>();
        Iterator<VirtualNode<T>> it = ring.tailMap(hash).values().iterator();
        while (result.size() < replicasFactor.getFrom()) {
            if (!it.hasNext()) {
                it = ring.values().iterator();
            }
            result.add(it.next().physicalNode);
        }
        return result;
    }

    @Override
    public boolean isMe(@NotNull final T node) {
        return me.equals(node);
    }

    @NotNull
    @Override
    public Set<T> all() {
        return new HashSet<>(topology);
    }

    /** Add new node to hash ring.
     * @param node - current node
     * @param virtualNodeCount - virtual nodes for hash ring
     */
    @SuppressWarnings(BETA)
    public void addNode(@NotNull final T node,
                        final int virtualNodeCount) {
        for (var i = 0; i < virtualNodeCount; i++) {
            final VirtualNode<T> vnode = new VirtualNode<>(node, i);
            final byte[] vnodeBytes = vnode.getName().getBytes(Charsets.UTF_8);
            final long hash = hashFunction.hashBytes(vnodeBytes).asLong();
            ring.put(hash, vnode);
        }
    }

    private static class VirtualNode<T> {
        private final T physicalNode;
        private final int replicaIndex;

        VirtualNode(@NotNull final T physicalNode,
                    final int replicaIndex) {
            this.replicaIndex = replicaIndex;
            this.physicalNode = physicalNode;
        }

        String getName() {
            return physicalNode + "-" + replicaIndex;
        }
    }
}
