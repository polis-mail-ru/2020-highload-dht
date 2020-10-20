package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

public interface ShardingPolicy<KeyType, NodeType> {

    /**
     * Return node identifier by key.
     * @param key records's key
     * @return node identifier
     */
    @NotNull
    NodeType getNode(@NotNull KeyType key);

    /**
     * Method for getting all nodes.
     * @return node array
     */
    @NotNull
    NodeType[] all();

    /**
     * Method for getting home node.
     * @return home url
     */
    @NotNull
    NodeType homeNode();
}
