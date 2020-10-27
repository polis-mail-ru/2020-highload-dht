package ru.mail.polis.service.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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

    @Override
    public ArrayList<String> getReplNodes(@NotNull final String node, int count) {
        ArrayList<String> nodes = new ArrayList<>();
        List<String> list = Arrays.asList(all());

        for (Iterator<String> i = circularIterator(list, count, list.lastIndexOf(node)); i.hasNext();) {
            String s = i.next();
            nodes.add(s);
        }

        return nodes;
    }

    @Override
    public int size() {
        return circle.size();
    }

    @NotNull
    @Override
    public String[] all() {
        return new TreeSet<>(circle.values()).toArray(new String[0]);
    }

    static <T> Iterator<T> circularIterator(List<T> list, int count, int startPos) {
        int size = list.size();
        return new Iterator<T>() {

            int i = startPos;

            @Override
            public boolean hasNext() {
                return i < count;
            }

            @Override
            public T next() {
                return list.get(i++ % size);
            }
        };
    }
}
