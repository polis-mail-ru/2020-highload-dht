package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RocksDAOImpl implements DAO{
    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) throws IOException {
        return null;
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull ByteBuffer key) throws IOException, NoSuchElementException {
        return null;
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value) throws IOException {

    }

    @Override
    public void remove(@NotNull ByteBuffer key) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
