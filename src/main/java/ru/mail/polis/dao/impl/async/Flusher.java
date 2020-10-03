package ru.mail.polis.dao.impl.async;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.tables.Table;

public interface Flusher {
    void flush(final int generation, @NotNull final Table table);
}
