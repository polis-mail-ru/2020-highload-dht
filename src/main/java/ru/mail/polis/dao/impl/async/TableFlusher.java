package ru.mail.polis.dao.impl.async;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.dao.impl.tables.Table;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

@ThreadSafe
public final class TableFlusher implements Flusher, Closeable {

    private static final int THREAD_COUNT = 4;

    private final DAOImpl dao;
    private final Executor executor;
    private final Phaser phaser;

    public TableFlusher(@NotNull final DAOImpl dao) {
        this.dao = dao;
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
        this.phaser = new Phaser(1);
    }

    @Override
    public void flush(final int generation, @NotNull final Table table) {
        final var task = new FlushTask(generation, table);
        phaser.register();
        executor.execute(task);
    }

    @Override
    public void close() {
        phaser.arriveAndAwaitAdvance();
    }

    private class FlushTask implements Runnable {

        private final int generation;
        private final Table table;

        private FlushTask(final int generation, @NotNull final Table table) {
            this.generation = generation;
            this.table = table;
        }

        @Override
        public void run() {
            final Iterator<Cell> iterator;
            try {
                iterator = table.iterator(ByteBuffer.allocate(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
            phaser.arrive();
        }
    }
}
