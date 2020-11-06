package ru.mail.polis.service.s3ponia;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

public class ReplicationConfiguration {
    public final int acks;
    public final int replicas;

    public ReplicationConfiguration(final int ack, final int from) {
        this.acks = ack;
        this.replicas = from;
    }

    /**
     * Parses ReplicationConfiguration from String.
     *
     * @param s String for parsing
     * @return ReplicationConfiguration on null
     */
    public static ReplicationConfiguration parse(@NotNull final String s) {
        final var splitStrings = Splitter.on('/').splitToList(s);
        if (splitStrings.size() != 2) {
            return null;
        }

        try {
            return new ReplicationConfiguration(Integer.parseInt(splitStrings.get(0)),
                    Integer.parseInt(splitStrings.get(1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return acks + "/" + replicas;
    }
}
