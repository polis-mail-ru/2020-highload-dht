package ru.mail.polis.dao.alexander.marashov;

public class NumberedTable {
    public Table table;
    public int generation;

    public NumberedTable(final Table table, final int generation) {
        this.table = table;
        this.generation = generation;
    }
}
