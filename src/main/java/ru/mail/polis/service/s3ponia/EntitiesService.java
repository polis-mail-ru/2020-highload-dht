package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface EntitiesService {
    Iterator<Record> range(@NotNull final ByteBuffer from,
                           @NotNull final ByteBuffer to) throws IOException;

    Iterator<Record> from(@NotNull final ByteBuffer from) throws IOException;
}
