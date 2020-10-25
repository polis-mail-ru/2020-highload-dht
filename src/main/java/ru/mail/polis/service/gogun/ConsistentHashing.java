package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashing implements Hashing<String> {

    @NotNull
    private final String me;
    @NotNull
    private final NavigableMap<Integer, String> circle = new TreeMap<>();
    private final int numOfNodes;

    /**
     * Class provides sharding via consistent hashing.
     *
     * @param nodes - set of nodes
     * @param me    - current node
     */
    public ConsistentHashing(
            @NotNull final Collection<String> nodes,
            @NotNull final String me,
            final int vnodes) {
        numOfNodes = vnodes;
        this.me = me;

        for (final String node : nodes) {
            add(node);
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
            final Map.Entry<Integer, String> ceilingEntry = circle.ceilingEntry(hash);
            hash = ceilingEntry == null ? circle.firstKey() : ceilingEntry.getKey();
        }

        return circle.get(hash);
    }

    private void add(final String node) {
        for (int i = 0; i < numOfNodes; i++) {
            circle.put((node + i).hashCode(), node);
        }
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
