package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

public interface ShardingPolicy<K, N> {

    /**
     * Return node identifier by key.
     * @param key records's key
     * @return node identifier
     */
    @NotNull
    N getNode(@NotNull final K key);

    /**
     * Method for getting all nodes.
     * @return node array
     */
    @NotNull
    N[] all();

    /**
     * Method for getting home node.
     * @return home url
     */
    @NotNull
    N homeNode();
}
