package ru.mail.polis.service.nik27090;

import org.apache.commons.codec.digest.MurmurHash3;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class ModularTopology implements Topology<String> {
    @NotNull
    private final String[] nodes;
    @NotNull
    private final String me;

    public ModularTopology(@NotNull final Set<String> nodes,
                           @NotNull final String me) {
        this.me = me;
        assert nodes.contains(me);

        this.nodes = new String[nodes.size()];
        nodes.toArray(this.nodes);
        Arrays.sort(this.nodes);
    }

    @NotNull
    @Override
    public String primaryFor(@NotNull ByteBuffer key) {
        byte[] bytes = new byte[key.remaining()];
        key.duplicate().get(bytes);
        return nodes[(MurmurHash3.hash32x86(bytes) & Integer.MAX_VALUE) % nodes.length];
    }

    @Override
    public boolean isMe(@NotNull String node) {
        return node.equals(me);
    }

    @NotNull
    @Override
    public String[] all() {
        return nodes.clone();
    }
}
