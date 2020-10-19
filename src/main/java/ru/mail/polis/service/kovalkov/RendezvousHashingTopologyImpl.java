package ru.mail.polis.service.kovalkov;

import java.nio.ByteBuffer;

public class RendezvousHashingTopologyImpl implements Topology<String>{
    @Override
    public String identifyByKey(ByteBuffer key) {
        throw new RuntimeException("Not implement yet");
    }

    @Override
    public int nodeCount() {
        throw new RuntimeException("Not implement yet");
    }

    @Override
    public String[] allNodes() {
        throw new RuntimeException("Not implement yet");
    }

    @Override
    public boolean isMe(String node) {
        return false;
    }
}
