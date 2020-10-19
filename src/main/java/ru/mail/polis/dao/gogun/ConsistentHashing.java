package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing implements Hashing<String> {

    private final int numOfNodes;
    @NotNull
    private final String me;
    @NotNull
    private final SortedMap<Integer, String> circle = new TreeMap<>();

    /**
     * Class provides sharding via consistent hashing.
     *
     * @param nodes - set of nodes
     * @param me    - current node
     */
    public ConsistentHashing(
            @NotNull final Collection<String> nodes,
            @NotNull final String me) {
        this.numOfNodes = nodes.size();
        this.me = me;

        for (final String node : nodes) {
            for (int i = 0; i < numOfNodes; i++) {
                circle.put((node + i).hashCode(), node);
            }
        }
    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public String get(@NotNull final ByteBuffer key) {
        int hash = key.hashCode();

        if (!circle.containsKey(hash)) {
            final SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }

        return circle.get(hash);
    }

    @Override
    public int size() {
        return circle.size();
    }

    @NotNull
    @Override
    public Collection<String> all() {
        return circle.values();
    }
}
