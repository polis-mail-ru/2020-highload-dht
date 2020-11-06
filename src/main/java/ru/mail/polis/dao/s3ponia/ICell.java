package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface ICell extends Comparable<ICell> {
    @NotNull
    ByteBuffer getKey();

    @NotNull
    Value getValue();
}
