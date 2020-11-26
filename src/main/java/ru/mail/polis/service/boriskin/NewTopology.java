package ru.mail.polis.service.boriskin;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Визуализация:
 * http://felixgv.github.io/voldemort/images/hash_ring.png .
 * Consistent Hashing:
 * https://en.wikipedia.org/wiki/Consistent_hashing .
 */
public final class NewTopology implements ConsistentHashingTopology {

    private static final int PARTITIONS_COUNT = 32;
    private static final int NUMERIC_OVERFLOW_IN_LONG_AVOIDER = 1;
    // (Math.pow(2, HASH_BITS) / PARTITIONS_COUNT) when HASH_BITS == 64
    private static final long HASH_BITS_POW_BY_PARTITIONS_COUNT = 576460752303423488L;
    // Math.pow(2, HASH_BITS - 1) - 1 when HASH_BITS == 64
    private static final long HASH_BITS_POW = Long.MAX_VALUE;

    @NotNull
    private final NavigableSet<String> nodeSet;
    @NotNull
    private final String myNode;

    @NotNull
    private final NavigableMap<Long, Node> ring;
    @NotNull
    private final HashFunction hashFunction;

    /**
     * Конструктор {@link NewTopology}.
     *
     * @param nodeSet узлы
     * @param myNode нужно знать себя
     */
    public NewTopology(
            @NotNull final Set<String> nodeSet,
            @NotNull final String myNode) {
        assert nodeSet.contains(myNode);
        this.myNode = myNode;

        this.nodeSet = new TreeSet<>(nodeSet);

        this.ring = new TreeMap<>();
        this.hashFunction = Hashing.murmur3_128();
        generateVNodes(new ArrayList<>(this.nodeSet));
    }

    private void generateVNodes(
            @NotNull final List<String> nodeSet) {
        for (int i = 0; i < PARTITIONS_COUNT; i++) {
            final long token =
                    HASH_BITS_POW_BY_PARTITIONS_COUNT * i
                            - HASH_BITS_POW - NUMERIC_OVERFLOW_IN_LONG_AVOIDER;
            ring.put(token, new Node(token, nodeSet.get(i % nodeSet.size())));
        }
    }

    @NotNull
    @Override
    public String primaryFor(
            @NotNull final ByteBuffer key) {
        return getFirstOne(key)
                .getValue()
                .getAddress();
    }

    @NotNull
    @Override
    public List<String> replicas(
            @NotNull final ByteBuffer key,
            final int from) {
        if (from > nodeSet.size()) {
            throw new IllegalArgumentException(
                    "Неверный RF:"
                            + "[point = " + from + "] > [ nodeSetSize = " + all().size());
        }

        final ArrayList<String> replicas = new ArrayList<>();
        final Map.Entry<Long, Node> firstReplica = getFirstOne(key);

        replicas.add(firstReplica.getValue().getAddress());
        int cntReplicas = 1;

        Iterator<Node> iterator =
                ring
                        .tailMap(firstReplica.getKey())
                        .values()
                        .iterator();

        while (cntReplicas != from) {
            if (!iterator.hasNext()) {
                iterator = ring.values().iterator();
            }
            final Node vnode = iterator.next();
            final String replica = vnode.getAddress();
            if (!replicas.contains(replica)) {
                replicas.add(replica);
                cntReplicas++;
            }
        }
        return replicas;
    }

    @NotNull
    private Map.Entry<Long, Node> getFirstOne(
            @NotNull final ByteBuffer key) {
        final long hashKey = hashFunction.hashBytes(key).asLong();
        final Map.Entry<Long, Node> entry = ring.ceilingEntry(hashKey);
        return entry == null
                ? ring.firstEntry() : entry;
    }

    @Override
    public boolean isMyNode(
            @NotNull final String node) {
        return myNode.equals(node);
    }

    @NotNull
    @Override
    public String recogniseMyself() {
        return myNode;
    }

    @NotNull
    @Override
    public Set<String> all() {
        return Set.copyOf(nodeSet);
    }

    @Override
    public boolean add(
            @NotNull final String node) {
        if (nodeSet.contains(node)) {
            return false;
        }
        nodeSet.add(node);
        final List<Node> vNodes =
                randomise(ring.values(), PARTITIONS_COUNT / nodeSet.size());
        for (final Node vn : vNodes) {
            vn.setAddress(node);
        }
        return true;
    }

    @Override
    public boolean remove(
            @NotNull final String node) {
        if (!nodeSet.contains(node)) {
            return false;
        }
        nodeSet.remove(node);
        ring.replaceAll((t, vn) -> getNode(node, vn));
        return true;
    }

    @NotNull
    private Node getNode(
            @NotNull final String node,
            final Node vn) {
        if (node.equals(vn.getAddress())) {
            vn.setAddress(randomise(all(), 1).get(0));
        }
        return vn;
    }

    private static <N> List<N> randomise(
            @NotNull final Collection<N> coll,
            final int count) {
        final ArrayList<N> elements = new ArrayList<>(coll);
        Collections.shuffle(elements);
        return elements.subList(0, count);
    }
}
