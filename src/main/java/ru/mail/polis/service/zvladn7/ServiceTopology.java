package ru.mail.polis.service.zvladn7;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ServiceTopology implements Topology<String> {

    private static final int VIRTUAL_NODES_PER_NODE = 10;

    @NotNull
    private final String local;
    @NotNull
    private final SortedMap<Integer, String> hashRing;

    public ServiceTopology(@NotNull final Set<String> nodeSet, @NotNull final String local) {
        assert nodeSet.contains(local);
        this.local = local;
        this.hashRing = new TreeMap<>();

        for (String node : nodeSet) {
            attachToRing(node);
        }
    }

    @NotNull
    @Override
    public String nodeFor(@NotNull final ByteBuffer key) {
        int hash = key.hashCode();
        if (!hashRing.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = hashRing.tailMap(hash);
            hash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
        }
        return hashRing.get(hash);
    }

    @Override
    public boolean isLocal(@NotNull final String node) {
        return node.equals(local);
    }

    @Override
    public int size() {
        return hashRing.size();
    }

    @Override
    public String[] nodes() {
        return hashRing.values()
                .stream()
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public String local() {
        return local;
    }

    public void attachToRing(@NotNull final String node) {
        for (int i = 0; i < VIRTUAL_NODES_PER_NODE; ++i) {
            hashRing.put((node + i).hashCode(), node);
        }
    }

    public void detachFromRing(@NotNull final String node) {
        for (int i = 0; i < VIRTUAL_NODES_PER_NODE; ++i) {
            hashRing.remove((node + i).hashCode());
        }
    }
}
