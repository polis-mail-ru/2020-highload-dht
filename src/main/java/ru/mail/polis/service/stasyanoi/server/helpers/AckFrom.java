package ru.mail.polis.service.stasyanoi.server.helpers;

public class AckFrom {

    private final int ack;
    private final int from;

    public AckFrom(final int ack, final int from) {
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
