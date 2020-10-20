package ru.mail.polis.service.alexander.marashov.topologies;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Stream;

public class RendezvousTopology implements Topology<String> {

    @NotNull
    final Collection<String> nodes;
    final String local;

    public RendezvousTopology(@NotNull final SortedSet<String> nodes, @NotNull final String local) {
        assert nodes.size() > 0;
        assert nodes.contains(local);

        this.nodes = nodes;
        this.local = local;
    }

    @Override
    public boolean isLocal(final String node) {
        return node.equals(local);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull final ByteBuffer key) {
        final Stream<Map.Entry<Integer, String>> sortedStream = nodes.stream()
                .map((nodeString) -> {
                    final int hashCode = key.hashCode() + nodeString.hashCode();
                    return Map.entry(hashCode & Integer.MAX_VALUE, nodeString);
                })
                .sorted(Map.Entry.comparingByKey());
        return sortedStream.findFirst().get().getValue();
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return nodes.iterator();
    }

    @Override
    public int size() {
        return nodes.size();
    }
}
