package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    private final Set<String> uniqueValues;

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

        this.uniqueValues = new TreeSet<>(nodes);
    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    private void add(final String node) {
        for (int i = 0; i < vnodes; i++) {
            circle.put((node + i).hashCode(), node);
        }
    }

    @NotNull
    @Override
    public Set<String> primaryFor(@NotNull final ByteBuffer key, final int count) {
        if (count > uniqueValues.size()) {
            throw new InvalidParameterException("Wrong count number");
        }
        final int hash = key.hashCode();
        final Set<String> result = new HashSet<>();
        final Collection<String> values = circle.tailMap(hash).values();
        var iterator = new TreeSet<>(values).iterator();
        while (result.size() < count) {
            if (!iterator.hasNext()) {
                iterator = uniqueValues.iterator();
            }

            result.add(iterator.next());
        }

        return result;
    }

    @Override
    public int size() {
        return circle.size();
    }

    @NotNull
    @Override
    public List<String> all() {
        return Arrays.asList(uniqueValues.toArray(new String[0]));
    }
}
