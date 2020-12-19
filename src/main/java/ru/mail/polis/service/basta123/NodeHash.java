package ru.mail.polis.service.basta123;

import org.jetbrains.annotations.NotNull;

public class NodeHash implements Comparable<NodeHash>{
   private String node;
   private int hash;

    public NodeHash(String node, int hash) {
        this.node = node;
        this.hash = hash;
    }

    @Override
    public int compareTo(@NotNull NodeHash o) {
        if (o.hash > hash)
            return -1;
        else if (o.hash < hash)
            return 1;
        else
            return 0;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

}
