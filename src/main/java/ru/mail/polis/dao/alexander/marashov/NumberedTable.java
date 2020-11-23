package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.Nullable;

public class NumberedTable {
    public Table table;
    public int generation;

    public NumberedTable(@Nullable final Table table, final int generation) {
        this.table = table;
        this.generation = generation;
    }
}
