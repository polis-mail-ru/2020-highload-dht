package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Cell implements Comparable<Cell> {

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final Value value;

    Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
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

    @Override
    public int compareTo(@NotNull Cell cell) {
        final int cmp = key.compareTo(cell.getKey());

        final int result;
        if (cmp == 0) {
            result = Long.compare(cell.getValue().getTimeStamp(), value.getTimeStamp());
        } else {
            result = cmp;
        }
        return result;
    }
}
