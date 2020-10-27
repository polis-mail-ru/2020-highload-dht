package ru.mail.polis.service.nik27090;

public class AckFrom {
    private final int ack;
    private final int from;

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }

    public AckFrom(int ack, int from) {
        this.ack = ack;
        this.from = from;
    }
}
