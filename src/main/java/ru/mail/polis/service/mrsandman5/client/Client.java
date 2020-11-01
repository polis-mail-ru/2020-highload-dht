package ru.mail.polis.service.mrsandman5.client;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;

import java.util.concurrent.CompletableFuture;

public interface Client {

    @NotNull
    CompletableFuture<Entry> getAsync(@NotNull final String id);

    @NotNull
    CompletableFuture<Void> putAsync(@NotNull final String id,
                                     @NotNull final byte[] data);

    @NotNull
    CompletableFuture<Void> deleteAsync(@NotNull final String id);
}
