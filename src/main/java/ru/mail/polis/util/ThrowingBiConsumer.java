package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws IOException;

    /**
     * Method that convert ThrowingBiConsumer to BiConsumer by adding IOException handler.
     * @param handler IOException handler
     * @return BiConsumer
     */
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
