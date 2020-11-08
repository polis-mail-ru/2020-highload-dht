package ru.mail.polis.service.ivanovandrey;

import com.google.common.base.Splitter;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.Timestamp;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static one.nio.http.Request.METHOD_GET;

public class Replica {
    private final int ackCount;
    private final int fromCount;

    /**
     * Constructor that works with "replicas".
     *
     * @param replicas - the value of 'replicas'-param.
     */
    public Replica(@NotNull final String replicas) {
        final List<String> r = Splitter.on('/').splitToList(replicas);
        this.ackCount = Integer.parseInt(r.get(0));
        this.fromCount = Integer.parseInt(r.get(1));
    }

    /**
     * Constructor that works with cluster size.
     *
     * @param nodeCount number of cluster nodes
     */
    public Replica(final int nodeCount) {
        this.ackCount = nodeCount / 2 + 1;
        this.fromCount = nodeCount;
    }

    public int getFromCount() {
        return fromCount;
    }

    public int getAckCount() {
        return ackCount;
    }

    /**
     * Form final response to client.
     *
     * @param responses - collection of responses.
     * @param method - method of the request.
     */
    public Response formFinalResponse(final Collection<Response> responses, final int method) {
        if (method == METHOD_GET) {
            return responses.stream()
                    .filter(response -> response.getStatus() == 200)
                    .max(Comparator.comparingLong(response ->
                            Timestamp.getTimestampByData(ByteBuffer.wrap(response.getBody()))
                                    .getTimestampValue()))
                    .map(response -> {
                        final Timestamp timestamp = Timestamp.getTimestampByData(
                                ByteBuffer.wrap(response.getBody()));
                        if (timestamp.getState() == Timestamp.State.DELETED) {
                            return new Response(Response.NOT_FOUND, Response.EMPTY);
                        } else {
                            return new Response(Response.OK, timestamp.getData());
                        }
                    })
                    .orElse(new Response(Response.NOT_FOUND, Response.EMPTY));

        }
        return responses.stream()
                .findFirst()
                .orElse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }
}
