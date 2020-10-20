package ru.mail.polis.service.mrsandman5.clustering;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Implemented by example - https://github.com/Jaskey/ConsistentHash.
 * */
public class ConsistentHashingTopology<T> implements Topology<T> {

    private final T me;
    @NotNull
    private final Set<T> topology;
    private final NavigableMap<Long, VirtualNode<T>> ring = new TreeMap<>();
    @SuppressWarnings("UnstableApiUsage")
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

    @SuppressWarnings("UnstableApiUsage")
    @NotNull
    @Override
    public T primaryFor(@NotNull final ByteBuffer key) {
        final var hash = hashFunction.hashBytes(key.duplicate()).asLong();
        final var nodeEntry = ring.ceilingEntry(hash);
        return nodeEntry == null
                ? ring.firstEntry().getValue().getPhysicalNode()
                : nodeEntry.getValue().getPhysicalNode();
    }

    @Override
    public boolean isNotMe(@NotNull final T node) {
        return !me.equals(node);
    }

    @NotNull
    @Override
    public Set<T> all() {
        return topology;
    }

    /** Add new node to hash ring.
     * @param node - current node
     * @param virtualNodeCount - virtual nodes for hash ring
     */
    @SuppressWarnings("UnstableApiUsage")
    public void addNode(@NotNull final T node,
                        final int virtualNodeCount) {
        for (var i = 0; i < virtualNodeCount; i++) {
            final var vnode = new VirtualNode<>(node, i);
            final var vnodeBytes = vnode.getName().getBytes(Charsets.UTF_8);
            final var hash = hashFunction.hashBytes(vnodeBytes).asLong();
            ring.put(hash, vnode);
        }
    }

    /** Find all replicas in hash ring.
     * @param node - current node
     */
    public int getExistingReplicas(@NotNull final T node) {
        final AtomicInteger replicas = new AtomicInteger(0);
        ring.values().forEach(virtualNode -> {
            if (virtualNode.isVirtualNodeOf(node)) {
                replicas.addAndGet(1);
            }
        });
        return replicas.get();
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

        boolean isVirtualNodeOf(@NotNull final T node) {
            return physicalNode.equals(node);
        }

        T getPhysicalNode() {
            return physicalNode;
        }
    }
}
