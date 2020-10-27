package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;

public final class Cell {

    public static final Comparator<Cell> COMPARATOR =
            Comparator.comparing(Cell::getKey);

    @NotNull
    private final ByteBuffer key;

    @NotNull
    private final Value value;

    public Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    public ByteBuffer getKey() {
        return key;
    }

    @NotNull
    public Value getValue() {
        return value;
    }
}
