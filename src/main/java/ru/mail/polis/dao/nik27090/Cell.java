package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class Cell {
    static final Comparator<Cell> COMPARATOR =
            Comparator.comparing(Cell::getKey);

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final Value value;

    public Cell(
            @NotNull final ByteBuffer key,
            @NotNull final Value value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    public ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    @NotNull
    public Value getValue() {
        return value;
    }
}
