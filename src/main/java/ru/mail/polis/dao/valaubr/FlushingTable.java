package ru.mail.polis.dao.valaubr;

import org.jetbrains.annotations.NotNull;

public class FlushingTable {
    private final Table table;
    private final int gen;
    private final boolean poisonPill;

    /**
     * Table to flush and we understand isPoisonPill.
     *
     * @param table - table to flush
     * @param gen - her generation
     * @param poisonPill - is bad
     */
    public FlushingTable(@NotNull final Table table,
                         @NotNull final int gen,
                         @NotNull final boolean poisonPill) {
        this.table = table;
        this.gen = gen;
        this.poisonPill = poisonPill;
    }

    /**
     * Table to flush, poison pill only false.
     *
     * @param table - table to flush
     * @param gen - her gen
     */
    public FlushingTable(@NotNull final Table table,
                         @NotNull final int gen) {
        this.table = table;
        this.gen = gen;
        this.poisonPill = false;
    }

    public int getGen() {
        return gen;
    }

    public Table getTable() {
        return table;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}
