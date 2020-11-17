package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

class AckFrom {

    private int ack;
    private int from;

    public AckFrom() {
    }

    /**
     * class instance const.
     */


    AckFrom(@NotNull final Topology<String> topology) {
        this.ack = topology.getSize() / 2 + 1;
        this.from = topology.getSize();
    }

    int getAckValue() {
        return ack;
    }

    void setAckValue(final int ack) {
        this.ack = ack;
    }

    int getFromValue() {
        return from;
    }

    void setFromValue(final int from) {
        this.from = from;
    }
}
