package ru.mail.polis.service.gogun;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;
import java.util.List;

public final class ReplicasFactor {
    private static final String EXCEPTION_TEXT = "Wrong ack and from";
    private final int ack;
    private final int from;

    /**
     * Class provides replication factor.
     *
     * @param replicas - replication factor
     */
    private ReplicasFactor(@NotNull final String replicas) {
        final List<String> askFrom = Splitter.on('/').splitToList(replicas);
        try {
            this.ack = Integer.parseInt(askFrom.get(0));
            this.from = Integer.parseInt(askFrom.get(1));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidParameterException(e.getMessage());
        }

        if (this.ack == 0 || this.ack > this.from) {
            throw new InvalidParameterException(EXCEPTION_TEXT);
        }
    }

    private ReplicasFactor(final int size) {
        this.from = size;
        this.ack = this.from / 2 + 1;

        if (this.ack == 0 || this.ack > this.from) {
            throw new InvalidParameterException(EXCEPTION_TEXT);
        }
    }

    public static ReplicasFactor quorum(@NotNull final String replicas) {
        return new ReplicasFactor(replicas);
    }

    public static ReplicasFactor quorum(final int size) {
        return new ReplicasFactor(size);
    }

    public int getAck() {
        return this.ack;
    }

    public int getFrom() {
        return this.from;
    }
}
