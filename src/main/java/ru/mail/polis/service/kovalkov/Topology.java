package ru.mail.polis.service.kovalkov;

public interface Topology<I> {
    I identifyByKey(byte[] key);

    int nodeCount();

    I[] allNodes();

    boolean isMe(I node);

}
