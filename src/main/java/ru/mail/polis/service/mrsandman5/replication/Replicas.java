package ru.mail.polis.service.mrsandman5.replication;

public class Replicas {

    private final int ack;
    private final int from;

    public Replicas(final int ack,
                    final int from) {
        this.ack = ack;
        this.from = from;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
