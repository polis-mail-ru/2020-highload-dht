package ru.mail.polis.service.kate.moreva;

import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class ConsistentTopology implements Topology<String> {
    private final Set<String> nodes;
    private final String me;
    private final NavigableMap<Integer, String> nodesRing = new TreeMap<>();

    /**
     * Consistent topology constructor.
     *
     * @param nodes - cluster.
     * @param me - name of the current node.
     * @param nodesNumber - number of virtual nodes.
     */
    public ConsistentTopology(final Set<String> nodes, final String me, final int nodesNumber) {
        this.me = me;
        assert nodes.contains(me);
        this.nodes = nodes;
        nodes.forEach(node -> addNode(node, nodesNumber));
    }

    private void addNode(final String node, final int nodeCount) {
        for (var i = 0; i < nodeCount; i++) {
            final StringBuilder vnode = new StringBuilder(node);
            vnode.append(i);
            int hashFunction = Hash.murmur3(vnode.toString());
            while (nodesRing.containsKey(hashFunction)) {
                vnode.append(i);
                hashFunction = Hash.murmur3(vnode.toString());
            }
            nodesRing.put(hashFunction, node);
        }
    }

    @NotNull
    @Override
    public Set<String> primaryFor(@NotNull final ByteBuffer key, final Replicas replicas,
                                  final int ask) throws IllegalArgumentException {
        if (nodes.size() < replicas.getFrom() || ask > replicas.getFrom()) {
            throw new IllegalArgumentException("Unable to parse request. Too many nodes asked.");
        }
        byte[] body;
        if (key.duplicate().hasRemaining()) {
            body = new byte[key.duplicate().remaining()];
            key.duplicate().get(body);
        } else {
            body = new byte[0];
        }
        final int hash = Hash.murmur3(body, 0, body.length);
        final Set<String> result = new HashSet<>();
        final Collection<String> values = nodesRing.tailMap(hash).values();
        Iterator<String> iterator = values.iterator();
        while (result.size() < replicas.getFrom()) {
            if (!iterator.hasNext()) {
                iterator = nodesRing.values().iterator();
            }
            result.add(iterator.next());
        }
        return result;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public List<String> all() {
        return new ArrayList<>(nodes);
    }
}
