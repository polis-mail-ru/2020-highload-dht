package ru.mail.polis.dao.kate.moreva;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class Cell implements Comparable<Cell> {

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final Value value;

    Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
        this.key = key;
        this.value = value;
    }

    Cell(@NotNull final ByteBuffer key, @Nullable final ByteBuffer value) {
        this.key = key;
        this.value = new Value(value);
    }

    @NotNull
    public ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    @NotNull
    public Value getValue() {
        return value;
    }

    @Override
    public int compareTo(@NotNull final Cell cell) {
        final int resultCmp = key.compareTo(cell.key);
        return resultCmp == 0 ? Long.compare(cell.getValue().getTimestamp(), value.getTimestamp()) : resultCmp;
    }
}
