package ru.mail.polis.service.nik27090;

import org.apache.commons.codec.digest.MurmurHash3;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

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
    public String getRightNodeForKey(@NotNull final ByteBuffer key) {
        String rightNode = null;

        int max = Integer.MIN_VALUE;
        for (final String node : nodes) {
            final int hashCode = MurmurHash3.hash32x86(getBytes(key))
                    + MurmurHash3.hash32x86(node.getBytes(StandardCharsets.UTF_8));
            if (hashCode > max) {
                max = hashCode;
                rightNode = node;
            }
        }
        if (rightNode == null) {
            throw new IllegalStateException("Can't choose right node!");
        } else {
            return rightNode;
        }
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

    private byte[] getBytes(final ByteBuffer byteBuffer) {
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.duplicate().get(bytes);
        return bytes;
    }
}
