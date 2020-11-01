package ru.mail.polis.service.mrsandman5.client;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.service.mrsandman5.replication.SimpleRequests;
import ru.mail.polis.utils.ByteUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LocalClient implements Client {

    private final Executor executor;
    private final SimpleRequests simpleRequests;

    public LocalClient(@NotNull final Executor executor,
                        @NotNull final SimpleRequests simpleRequests) {
        this.executor = executor;
        this.simpleRequests = simpleRequests;
    }

    @NotNull
    @Override
    public CompletableFuture<Entry> getAsync(@NotNull final String id) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        return CompletableFuture.supplyAsync(() -> simpleRequests.get(key), executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> putAsync(@NotNull final String id,
                                            @NotNull final byte[] data) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        return CompletableFuture.supplyAsync(() -> {
            simpleRequests.put(key, data);
            return null;
        });
    }

    @NotNull
    @Override
    public CompletableFuture<Void> deleteAsync(@NotNull final String id) {
        final ByteBuffer key = ByteUtils.getWrap(id);
        return CompletableFuture.supplyAsync(() -> {
            simpleRequests.delete(key);
            return null;
        });
    }
}
