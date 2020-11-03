package ru.mail.polis.util;

import com.google.common.base.Splitter;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.dariagap.Timestamp;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static one.nio.http.Request.METHOD_GET;

public class Replicas {
    private final int ack;
    private final int from;

    private static final int STATUS_OK = 200;

    /**
     * Config "ack" and "from" from query-parameter "replicas".
     *
     * @param replicas in format ack/from
     */
    public Replicas(@NotNull final String replicas) {
        final List<String> r = Splitter.on('/').splitToList(replicas);
        this.ack = Integer.parseInt(r.get(0));
        this.from = Integer.parseInt(r.get(1));
    }

    /**
     * Config "ack" and "from" from cluster size.
     *
     * @param from number of cluster nodes
     */
    public Replicas(@NotNull final int from) {
        this.ack = from / 2 + 1;
        this.from = from;
    }

    public int getFrom() {
        return from;
    }

    public int getAck() {
        return ack;
    }

    /**
     * Form final response to client.
     *
     * @param responses - collection of responses
     * @param method - method of the request
     */
    public Response formFinalResponse(final Collection<Response> responses, final int method) {
        if (method == METHOD_GET) {
            return responses.stream()
                    .filter(response -> response.getStatus() == STATUS_OK)
                    .max(Comparator.comparingLong(response ->
                        Timestamp.getTimestampByData(ByteBuffer.wrap(response.getBody()))
                                .getTimestampValue()))
                    .map(response -> {
                        final Timestamp timestamp = Timestamp.getTimestampByData(
                                ByteBuffer.wrap(response.getBody()));
                        if (timestamp.getState() == Timestamp.State.DELETED) {
                            return new Response(Response.NOT_FOUND, Response.EMPTY);
                        } else {
                            return new Response(Response.OK,timestamp.getData());
                        }
                    })
                    .orElse(new Response(Response.NOT_FOUND, Response.EMPTY));

        }
        return responses.stream().findFirst().get();
    }
}
