package ru.mail.polis.service.gogun;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReplicasFactor {

    private final int ack;
    private final int from;

    /**
     * Class provides replication factor.
     *
     * @param replicas - replication factor
     */
    public ReplicasFactor(@NotNull final String replicas) {
        final List<String> askFrom = Splitter.on('/').splitToList(replicas);
        this.ack = Integer.parseInt(askFrom.get(0).substring(1));
        this.from = Integer.parseInt(askFrom.get(1));
    }

    public ReplicasFactor(final int size) {
        this.from = size;
        this.ack = this.from / 2 + 1;
    }

    public boolean isBad() {
        return this.ack == 0 || this.ack > this.from;
    }

    public int getAck() {
        return this.ack;
    }

    public int getFrom() {
        return this.from;
    }
}
