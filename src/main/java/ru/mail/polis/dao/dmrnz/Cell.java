package ru.mail.polis.dao.dmrnz;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;

final class Cell {
    static final Comparator<Cell> COMPARATOR =
            Comparator.comparing(Cell::getKey).thenComparing(Cell::getValue);

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final Value value;

    Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    @NotNull
    Value getValue() {
        return value;
    }
}
