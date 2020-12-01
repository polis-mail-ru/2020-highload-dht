package ru.mail.polis.dao.alexander.marashov.compactor;

import ru.mail.polis.dao.alexander.marashov.Table;

import java.io.File;
import java.util.NavigableMap;

public interface CompactorCallback {
    void compactionDone(
            final NavigableMap<Integer, Table> compactedTables,
            final Integer maxGeneration,
            final File compactedTo
    );
}
