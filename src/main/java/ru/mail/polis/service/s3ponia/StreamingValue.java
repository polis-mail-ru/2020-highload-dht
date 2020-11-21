package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface StreamingValue {

    int valueSize();

    void value(@NotNull final ByteBuffer out);
}
