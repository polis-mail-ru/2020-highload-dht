package ru.mail.polis.service.ivanovandrey;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

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

    public static Request getSpecialRequest(final Request request) {
        if (request.getParameter("special") != null) {
            return request;
        }
        final var newURI = request.getURI() + "&special=";
        final var res = new Request(request.getMethod(), newURI, request.isHttp11());
        for (int i = 0; i < request.getHeaderCount(); i++) {
            res.addHeader(request.getHeaders()[i]);
        }
        res.setBody(request.getBody());
        return res;
    }
}
