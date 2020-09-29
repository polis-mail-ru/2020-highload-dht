package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.Utils.BufferConverter;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RocksToDAOAdapter extends RocksDBImpl implements DAO {

    public RocksToDAOAdapter(File data) {
        super(data);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) {
        return iterator(BufferConverter.convertBuffer(from));
    }

    @NotNull
    @Override
    public ByteBuffer get(@NotNull ByteBuffer key) throws NoSuchElementException {
        return get(BufferConverter.convertBuffer(key));
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value){
        put(BufferConverter.convertBuffer(key), BufferConverter.unfoldToBytes(value));
    }

    @Override
    public void remove(@NotNull ByteBuffer key){
        delete(BufferConverter.convertBuffer(key));
    }
}
