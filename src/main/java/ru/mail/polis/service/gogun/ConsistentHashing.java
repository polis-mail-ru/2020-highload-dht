package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConsistentHashing implements Hashing<String> {

    @NotNull
    private final String me;
    @NotNull
    private final NavigableMap<Integer, String> circle = new TreeMap<>();
    private final int vnodes;

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
        this.vnodes = vnodes;
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
        for (int i = 0; i < vnodes; i++) {
            circle.put((node + i).hashCode(), node);
        }
    }

    @NotNull
    @Override
    public Set<String> getReplNodes(@NotNull final String startNode, final int count) {
        final Set<String> nodes = new HashSet<>();
        final Set<String> set = new TreeSet<>(circle.values());
        int counter = count;
        Iterator<String> iterator = set.iterator();

        while (iterator.hasNext()) {
            final String node = iterator.next();
            if (node.equals(startNode) || !nodes.isEmpty()) {
                nodes.add(node);
                counter--;
            }

            if (counter == 0) {
                break;
            }

            if (!iterator.hasNext()) {
                iterator = set.iterator();
            }
        }

        return nodes;
    }

    @Override
    public int size() {
        return circle.size();
    }

    @NotNull
    @Override
    public List<String> all() {
        return Arrays.asList(new TreeSet<>(circle.values()).toArray(new String[0]));
    }
}
