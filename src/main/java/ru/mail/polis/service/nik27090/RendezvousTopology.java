package ru.mail.polis.service.nik27090;

import one.nio.http.Request;
import org.apache.commons.codec.digest.MurmurHash3;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RendezvousTopology implements Topology<String> {
    @NotNull
    private final String[] nodes;
    @NotNull
    private final String currentNode;

    /**
     * Rendezvous topology.
     *
     * @param nodes       - cluster.
     * @param currentNode - currentNode.
     */
    public RendezvousTopology(@NotNull final Set<String> nodes,
                              @NotNull final String currentNode) {
        this.currentNode = currentNode;
        assert nodes.contains(currentNode);

        this.nodes = new String[nodes.size()];
        nodes.toArray(this.nodes);
    }

    @NotNull
    @Override
    public String[] getReplicas(@NotNull ByteBuffer key, int countReplicas) {
        if (countReplicas == nodes.length) {
            return nodes;
        }

        Map<Integer, String> replicasHash = new HashMap<>();
        final String[] currentReplicas = new String[countReplicas];

        for (final String node : nodes) {
            final int hashCode = MurmurHash3.hash32x86(getBytes(key))
                    + MurmurHash3.hash32x86(node.getBytes(StandardCharsets.UTF_8));
            replicasHash.put(hashCode, node);
        }
        final List<Integer> sortedHash = replicasHash
                .keySet()
                .stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        for (int i = 0; i < countReplicas; i++) {
            currentReplicas[i] = replicasHash.get(sortedHash.get(i));
        }

        return currentReplicas;
    }

    @Override
    public boolean isCurrentNode(@NotNull final String node) {
        return node.equals(currentNode);
    }

    @NotNull
    @Override
    public String[] all() {
        return nodes.clone();
    }

    @Override
    @SuppressWarnings("StringSplitter")
    public AckFrom parseAckFrom(String ackFrom) {
        final int ack;
        final int from;
        if (ackFrom == null) {
            from = nodes.length;
            ack = from / 2 + 1;
        } else {
            final String[] ac = ackFrom.split("/");
            ack = Integer.parseInt(ac[0]);
            from = Integer.parseInt(ac[1]);
        }
        return new AckFrom(ack, from);
    }

    @Override
    public boolean isProxyReq(Request request) {
        return request.getHeader("X-Proxy-For:") != null;
    }

    private byte[] getBytes(final ByteBuffer byteBuffer) {
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.duplicate().get(bytes);
        return bytes;
    }

    @Override
    public int size() {
        return nodes.length;
    }
}
