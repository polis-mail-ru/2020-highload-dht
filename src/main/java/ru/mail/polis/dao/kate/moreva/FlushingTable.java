package ru.mail.polis.dao.kate.moreva;

/**
 * Table for flushing.
 * */
public class FlushingTable {

    private final Table table;
    private final int generation;
    private final boolean poisonPill;

    public FlushingTable(final Table table, final int generation, final boolean poisonPill) {
        this.table = table;
        this.generation = generation;
        this.poisonPill = poisonPill;
    }

    public FlushingTable(final Table table, final int generation) {
        this.table = table;
        this.generation = generation;
        this.poisonPill = false;
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
}
