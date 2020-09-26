package ru.mail.polis.dao.boriskin;

import java.nio.ByteBuffer;
import java.util.Comparator;

final class TableCell {

    private final ByteBuffer key;
    private final Value val;

    static final Comparator<TableCell> COMPARATOR = Comparator
            .comparing(TableCell::getKey)
            .thenComparing(TableCell::getValue);

    TableCell(final ByteBuffer key, final Value val) {
        this.key = key;
        this.val = val;
    }

    public ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    public Value getValue() {
        return val;
    }
}
