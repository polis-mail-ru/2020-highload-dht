package ru.mail.polis.service.suhova;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Replica {
    private final int ack;
    private final int from;

    public Replica(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }

    /**
     * Create Replica.
     *
     * @param replica replica
     * @return new Replica
     */
    @NotNull
    public static Replica of(@NotNull final String replica) {
        final List<String> values = Splitter.on('/').splitToList(replica);
        if (values.size() != 2) {
            throw new IllegalArgumentException("Incorrect replica parameter");
        }
        return new Replica(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)));
    }
}
