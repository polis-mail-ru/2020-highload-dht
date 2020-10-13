package ru.mail.polis.dao.valaubr;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class TablePool implements Table {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final NavigableMap<Integer, Table> writingFlushTables;
    private final BlockingQueue<FlushingTable> flushQueue;
    private final long memFlushThreshold;
    private final AtomicBoolean stopFlag = new AtomicBoolean();
    private MemTable current;
    private int generation;

    @NotNull
    @Override
    public Iterator<Cell> iterator(@NotNull ByteBuffer from) throws IOException {
        return null;
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {

    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {

    }

    @Override
    public long getSizeInByte() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void close() {

    }
}
