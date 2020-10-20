package ru.mail.polis.service.kovalkov;

import java.nio.ByteBuffer;

public class RendezvousHashingTopologyImpl implements Topology <String> {
    private static final String NIE = "Not implement yet";

    @Override
    public String identifyByKey(ByteBuffer key) {
        throw new RuntimeException(NIE);
    }

    @Override
    public int nodeCount() { throw new RuntimeException(NIE); }

    @Override
    public String[] allNodes() {
        throw new RuntimeException(NIE);
    }

    @Override
    public boolean isMe(String node){
        return false;
    }
}
