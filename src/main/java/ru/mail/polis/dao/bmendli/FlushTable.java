package ru.mail.polis.dao.bmendli;

import org.jetbrains.annotations.NotNull;

public class FlushTable {

    private final int generation;
    private final Table table;
    private final boolean poisonPill;

    public FlushTable(final int generation,
                      @NotNull final Table table) {
        this(generation, table, false);
    }

    /**
     * Table which waiting a flush.
     */
    public FlushTable(final int generation,
                      @NotNull final Table table,
                      final boolean poisonPill) {
        this.table = table;
        this.generation = generation;
        this.poisonPill = poisonPill;
    }

    public Table getTable() {
        return table;
    }

    public int getGeneration() {
        return generation;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }

    @Override
    public String toString() {
        return "FlushTable{"
                + "generation=" + generation
                + ", table=" + table
                + ", poisonPill=" + poisonPill + '}';
    }
}
