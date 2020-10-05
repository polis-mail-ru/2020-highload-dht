package ru.mail.polis.dao.kate.moreva;

public class FlushingTable {

    private final Table table;
    private final int generation;
    private final boolean poisonPill;

    public FlushingTable(Table table, int generation, boolean poisonPill) {
        this.table = table;
        this.generation = generation;
        this.poisonPill = poisonPill;
    }

    public FlushingTable(Table table, int generation) {
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
