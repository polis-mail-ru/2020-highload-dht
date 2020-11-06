package ru.mail.polis.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws IOException;

    default BiConsumer<T, U> toBiConsumer(@NotNull final BiConsumer<T, U> handler) {
        return (t, u) -> {
            try {
                accept(t, u);
            } catch (IOException e) {
                handler.accept(t, u);
            }
        };
    }
}
