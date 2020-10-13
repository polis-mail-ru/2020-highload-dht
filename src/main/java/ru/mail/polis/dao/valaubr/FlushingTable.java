package ru.mail.polis.dao.valaubr;

public class FlushingTable {
    private final Table table;
    private final int gen;
    private final boolean pPill;

    public FlushingTable(Table table, int gen, boolean pPill) {
        this.table = table;
        this.gen = gen;
        this.pPill = pPill;
    }

    public FlushingTable(Table table, int gen) {
        this.table = table;
        this.gen = gen;
        this.pPill = false;
    }

    public int getGen() {
        return gen;
    }

    public Table getTable() {
        return table;
    }

    public boolean ispPill() {
        return pPill;
    }
}
