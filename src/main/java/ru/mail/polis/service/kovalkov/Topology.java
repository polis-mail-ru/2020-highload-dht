package ru.mail.polis.service.kovalkov;

import java.nio.ByteBuffer;

public interface Topology <I>{
    I identifyByKey(ByteBuffer key);

    int nodeCount();

    I[] allNodes();

    boolean isMe(I node);

}
