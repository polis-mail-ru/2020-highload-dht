package ru.mail.polis.dao.impl.async;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.dao.impl.tables.Table;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class TableFlusher implements Flusher{

    private final DAOImpl dao;

    public TableFlusher(@NotNull final DAOImpl dao) {
        this.dao = dao;
    }

    @Override
    public void flush(int generation, @NotNull Table table) {

    }
}
