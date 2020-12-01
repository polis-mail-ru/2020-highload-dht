package ru.mail.polis.dao.alexander.marashov.compactor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.alexander.marashov.Cell;
import ru.mail.polis.dao.alexander.marashov.SSTable;
import ru.mail.polis.dao.alexander.marashov.Table;
import ru.mail.polis.dao.alexander.marashov.iterators.CellIterator;
import ru.mail.polis.dao.alexander.marashov.iterators.TableIterator;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static ru.mail.polis.dao.alexander.marashov.DAOImpl.SUFFIX;
import static ru.mail.polis.dao.alexander.marashov.DAOImpl.TEMP;
import static ru.mail.polis.dao.alexander.marashov.DAOImpl.UNDERSCORE;

public class Compactor extends Thread {

    private static final int COMPACTOR_QUEUE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(Compactor.class);

    public final BlockingQueue<CompactorTask> tasksQueue;

    private final File storage;
    private final CompactorCallback callback;
    final TablesSupplier tablesToCompactSupplier;

    /**
     * Compactor constructor.
     *
     * @param storage                 - root of the DAO storage.
     * @param tablesToCompactSupplier - supplier for getting tables that need to be compacted.
     * @param onCompactedCallback     - callback function which called after tables compacted.
     */
    public Compactor(
            final File storage,
            final TablesSupplier tablesToCompactSupplier,
            final CompactorCallback onCompactedCallback
    ) {
        super("Compactor");
        this.storage = storage;
        this.tasksQueue = new LinkedBlockingQueue<>(COMPACTOR_QUEUE_SIZE);
        this.tablesToCompactSupplier = tablesToCompactSupplier;
        this.callback = onCompactedCallback;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final CompactorTask task = tasksQueue.take();
                log.debug("Compact task queued");
                if (task.isPoisonPill()) {
                    log.info("Compactor stopped");
                    break;
                }

                final NavigableMap<Integer, Table> tablesToCompact = tablesToCompactSupplier.get();
                if (tablesToCompact.size() <= 1) {
                    log.info("Hmm. Nothing to compact. I'm gonna sleep again.");
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

                this.callback.compactionDone(tablesToCompact, maxGeneration, dst);

                deleteFiles(tablesToCompact.values());
                log.info("Compact done");
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
