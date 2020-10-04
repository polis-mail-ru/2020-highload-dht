package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * В данном задании реализация осуществляется согласно структуре, представленной в конце лекции.
 *
 * @author Makary Boriskin
 */
public interface Table {

    long getSize();

    @NotNull
    Iterator<TableCell> iterator(@NotNull ByteBuffer point) throws IOException;

    void upsert(@NotNull ByteBuffer key, @NotNull ByteBuffer val) throws IOException;

    void remove(@NotNull ByteBuffer key) throws IOException;
}
