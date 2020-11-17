package ru.mail.polis.service.kovalkov.replication;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ReplicationFactor {
    private static final Logger log = LoggerFactory.getLogger(ReplicationFactor.class);
    private final int ack;
    private final int from;

    public ReplicationFactor(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    /**
     * Get replication factor. He pars response by ask and wrap one.
     *
     * @param val request from client.
     * @param replicationFactor replication factor instance. If not exist return new.
     * @param session http session
     * @return new instance if does not exist
     * @throws IOException cause send error will be fault
     */
    @NotNull
    public static ReplicationFactor getReplicationFactor(@Nullable final String val,
                                                         @NotNull final ReplicationFactor replicationFactor,
                                                         @NotNull final HttpSession session) throws IOException {
        if (Objects.nonNull(val)) {
            final List<String> ackFromList = Arrays.asList(val.replace("=", "").split("/"));
            final int ack = Integer.parseInt(ackFromList.get(0));
            final int from = Integer.parseInt(ackFromList.get(1));
            if (ackFromList.size() != 2 || ack < 1 || from < 1 || ack > from) {
                final String msg = "Invalid ack/from factor";
                log.error(msg);
                session.sendError(Response.BAD_REQUEST, msg);
            }
            return new ReplicationFactor(ack,from);
        } else {
            return replicationFactor;
        }
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
