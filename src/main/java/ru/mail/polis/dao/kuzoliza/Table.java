package ru.mail.polis.dao.kuzoliza;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Table {

    @NotNull
    Iterator<Cell> iterator(@NotNull final ByteBuffer from) throws IOException;

    void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException;

    void remove(@NotNull ByteBuffer key) throws IOException;

}
