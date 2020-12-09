package ru.mail.polis.service.gogun;

import one.nio.util.Hash;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
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

    /**
     * Class provides sharding via consistent hashing.
     *
     * @param nodes - set of nodes
     * @param me    - current node
     */
    public ConsistentHashing(@NotNull final Collection<String> nodes,
            @NotNull final String me,
            final int vnodes) {
        this.me = me;

        for (final String node : nodes) {
            add(node, circle, vnodes);
        }

    }

    @Override
    public boolean isMe(@NotNull final String node) {
        return node.equals(me);
    }

    public static void add(final String node, final Map<Integer, String> circle, final int vnodes) {
        for (int i = 0; i < vnodes; ++i) {
            final StringBuilder stringToHash = new StringBuilder(node);
            stringToHash.append(i);
            int hashCode = Hash.murmur3(stringToHash.toString());
            while (circle.containsKey(hashCode)) {
                stringToHash.append(i);
                hashCode = Hash.murmur3(stringToHash.toString());
            }
            circle.put(hashCode, node);
        }
    }

    @NotNull
    @Override
    public Set<String> primaryFor(@NotNull final ByteBuffer key, final int count) {
        if (count > new TreeSet<>(circle.values()).size()) {
            throw new InvalidParameterException("Wrong count number");
        }

        final byte[] keyBytes = ServiceUtils.getArray(key.duplicate());
        final int hash = Hash.murmur3(keyBytes, 0, keyBytes.length);
        final Set<String> result = new HashSet<>();
        final Collection<String> values = circle.tailMap(hash).values();
        Iterator<String> iterator = values.iterator();
        while (result.size() < count) {
            if (!iterator.hasNext()) {
                iterator = circle.values().iterator();
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
    public NavigableMap<Integer, String> getCircle() {
        return circle;
    }

    @NotNull
    @Override
    public List<String> all() {
        return Arrays.asList(new TreeSet<>(circle.values()).toArray(new String[0]));
    }
}
