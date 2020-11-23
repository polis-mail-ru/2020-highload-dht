package ru.mail.polis.dao.alexander.marashov;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static ru.mail.polis.dao.alexander.marashov.DAOImpl.SUFFIX;
import static ru.mail.polis.dao.alexander.marashov.DAOImpl.TEMP;
import static ru.mail.polis.dao.alexander.marashov.DAOImpl.UNDERSCORE;

public class Compactor extends Thread {

    private static final int COMPACTOR_QUEUE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(Compactor.class);

    final BlockingQueue<CompactorTask> tasksQueue;

    private final File storage;
    private final Function<NavigableMap<Integer, Table>, Function<Integer, Consumer<File>>> tablesCompactedCallback;
    final Supplier<NavigableMap<Integer, Table>> tablesToCompactSupplier;

    /**
     * Compactor constructor.
     *
     * @param storage                 - root of the DAO storage.
     * @param tablesCompactedCallback - callback function which called after tables compacted.
     */
    public Compactor(
            final File storage,
            final Supplier<NavigableMap<Integer, Table>> tablesToCompactSupplier,
            final Function<NavigableMap<Integer, Table>, Function<Integer, Consumer<File>>> tablesCompactedCallback
    ) {
        super("Compactor");
        setDaemon(true);
        this.storage = storage;
        this.tasksQueue = new ArrayBlockingQueue<>(COMPACTOR_QUEUE_SIZE);
        this.tablesToCompactSupplier = tablesToCompactSupplier;
        this.tablesCompactedCallback = tablesCompactedCallback;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final CompactorTask task = tasksQueue.take();
                log.debug("COMPACTOR: compact task queued");
                if (task.isPoisonPill()) {
                    log.info("Compactor stopped");
                    break;
                }

                final NavigableMap<Integer, Table> tablesToCompact = tablesToCompactSupplier.get();
                if (tablesToCompact.size() <= 1) {
                    // nothing to compact
                    continue;
                }

                final int tableIteratorsCount = tablesToCompact.size();
                int maxGeneration = 0;
                final List<TableIterator> tableIteratorList = new ArrayList<>(tableIteratorsCount);
                for (final Map.Entry<Integer, Table> entry : tablesToCompact.entrySet()) {
                    maxGeneration = Math.max(maxGeneration, entry.getKey());
                    tableIteratorList.add(new TableIterator(entry.getKey(), entry.getValue()));
                }
                final Iterator<Cell> cellIterator = new CellIterator(tableIteratorList);
                final File file = new File(this.storage, maxGeneration + UNDERSCORE + TEMP);
                SSTable.serialize(cellIterator, file);
                final File dst = new File(this.storage, maxGeneration + UNDERSCORE + SUFFIX);
                Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);

                this.tablesCompactedCallback
                        .apply(tablesToCompact)
                        .apply(maxGeneration)
                        .accept(dst);

                deleteFiles(tablesToCompact.values());
            }
        } catch (final InterruptedException e) {
            log.error("Compactor interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (final IOException e) {
            log.error("Compactor met an unexpected IOException.", e);
            System.exit(-1);
        }
    }

    private static void deleteFiles(@NotNull final Collection<Table> tablesToRemove) throws IOException {
        for (final Table table : tablesToRemove) {
            final Path path = table.getFile().toPath();
            try {
                Files.delete(path);
            } catch (final IOException e) {
                throw new IOException("Compactor: can't delete file " + path, e);
            }
        }
    }
}
