package ru.mail.polis.service.ivanovandrey;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SimpleTopology {

    private final List<String> nodesList;
    private final String[] nodes;
    private final String me;

    /**
     * Constructor.
     *
     * @param topology - topology.
     * @param me - current node.
     */
    public SimpleTopology(@NotNull final Set<String> topology,
                          @NotNull final String me) {
        this.nodesList = Util.asSortedList(topology);
        this.nodes = new String[topology.size()];
        topology.toArray(this.nodes);
        this.me = me;
    }

    String getMe() {
        return this.me;
    }

    
    String primaryFor(@NotNull final ByteBuffer key){
        return nodes[(key.hashCode() & Integer.MAX_VALUE) % nodes.length];
    }
    String[] getNodes() {
        return nodes.clone();
    }
    List<String> responsibleNodes(@NotNull final String key,
                                  @NotNull final Replica replicas) {
       final int startIndex = (key.hashCode() & Integer.MAX_VALUE) % nodesList.size();
       final var res = new ArrayList<String>();
       for (int i = 0; i < replicas.getTotalNodes(); i++) {
           final int current = (startIndex + i) % nodesList.size();
           res.add(nodesList.get(current));
       }
       return res;
    }
}
