package ru.mail.polis.service.zvladn7;

class ReplicasHolder {
    int ack;
    int from;

    ReplicasHolder(final String replicas) {
        final int slashIndex = replicas.indexOf('/');
        this.ack = Integer.parseInt(replicas.substring(0, slashIndex));
        this.from = Integer.parseInt(replicas.substring(slashIndex + 1));
    }

    ReplicasHolder(final int from) {
        this.ack = from / 2 + 1;
        this.from = from;
    }
}
