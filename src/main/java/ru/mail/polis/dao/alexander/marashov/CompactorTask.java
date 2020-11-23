package ru.mail.polis.dao.alexander.marashov;

public class CompactorTask {

    private final boolean poisonPill;

    public CompactorTask(final boolean poissonPill) {
        this.poisonPill = poissonPill;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}
