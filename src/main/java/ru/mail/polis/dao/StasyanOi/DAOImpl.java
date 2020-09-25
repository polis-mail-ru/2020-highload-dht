package ru.mail.polis.dao.StasyanOi;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class DAOImpl implements DAO {
    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) throws IOException {
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
