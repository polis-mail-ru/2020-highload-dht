package ru.mail.polis.dao.kovalkov;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.Utils.BufferConverter;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class RocksToDAOAdapter extends RocksDBImpl implements DAO {

    public RocksToDAOAdapter(File data) {
        super(data);
    }

    @NotNull
    @Override
    public Iterator<Record> iterator(@NotNull ByteBuffer from) {
        return iterator(BufferConverter.unfoldToBytes(from));
    }

    @Override
    public void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer value){
        put(BufferConverter.unfoldToBytes(key), BufferConverter.unfoldToBytes(value));
    }

    @Override
    public void remove(@NotNull ByteBuffer key){
        delete(BufferConverter.unfoldToBytes(key));
    }
}
