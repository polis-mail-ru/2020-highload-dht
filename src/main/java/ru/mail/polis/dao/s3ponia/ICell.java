package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Comparator;

public interface ICell extends Comparable<ICell> {
    @NotNull
    ByteBuffer getKey();

    @NotNull
    Value getValue();

    @Override
    default int compareTo(@NotNull final ICell o) {
        return Comparator.comparing(ICell::getKey).thenComparing(ICell::getValue).compare(this, o);
    }
}
