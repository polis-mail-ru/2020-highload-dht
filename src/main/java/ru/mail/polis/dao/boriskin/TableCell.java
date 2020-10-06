package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class TableCell implements Comparable<TableCell> {

    private final ByteBuffer key;
    private final Value val;

    public TableCell(final ByteBuffer key, final Value val) {
        this.key = key;
        this.val = val;
    }

    @Override
    public int compareTo(@NotNull final TableCell tableCell) {
        final int cmp =
                key.compareTo(tableCell.getKey());

        final int result;
        if (cmp == 0) {
            result =
                    Long.compare(tableCell.getVal().getTimeStamp(), val.getTimeStamp());
        } else {
            result = cmp;
        }

        return result;
    }

    public ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    public Value getVal() {
        return val;
    }
}
