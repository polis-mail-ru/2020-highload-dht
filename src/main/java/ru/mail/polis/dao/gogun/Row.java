package ru.mail.polis.dao.gogun;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;

final class Row {

    public static final Comparator<Row> COMPARATOR =
            Comparator.comparing(Row::getKey).thenComparing(Row::getValue);

    @NotNull
    private final ByteBuffer key;
    @NotNull
    private final Value value;

    Row(@NotNull final ByteBuffer key, @NotNull final Value value) {
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
