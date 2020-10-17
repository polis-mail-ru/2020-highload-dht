package ru.mail.polis.dao.alexander.marashov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public class Flusher extends Thread {

    private static final int FLUSHER_QUEUE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(Flusher.class);

    final BlockingQueue<NumberedTable> tablesQueue;

    private final File storage;
    private final BiConsumer<Integer, File> tableFlashedCallback;

    /**
     * Flusher constructor.
     * @param storage - root of the DAO storage.
     * @param tableFlushedCallback - callback function which called after table flushing.
     */
    public Flusher(
            final File storage,
            final BiConsumer<Integer, File> tableFlushedCallback
    ) {
        super("Flusher");
        setDaemon(true);
        this.storage = storage;
        this.tablesQueue = new ArrayBlockingQueue<>(FLUSHER_QUEUE_SIZE);
        this.tableFlashedCallback = tableFlushedCallback;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final NumberedTable numberedTable = tablesQueue.take();
                log.debug("FLUSHER: flush task queued");
                if (numberedTable.table == null) {
                    log.info("Flusher stopped");
                    return;
                }
                final int generation = numberedTable.generation;
                final Iterator<Cell> cellIterator = numberedTable.table.iterator(ByteBuffer.allocate(0));
                final File file = new File(this.storage, generation + DAOImpl.TEMP);
                SSTable.serialize(cellIterator, file);
                final File dst = new File(this.storage, generation + DAOImpl.SUFFIX);
                Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
                this.tableFlashedCallback.accept(numberedTable.generation, dst);
            }
        } catch (final InterruptedException e) {
            log.error("Flusher interrupted. The program stops.", e);
            Thread.currentThread().interrupt();
        } catch (final IOException e) {
            log.error("Flusher met an unexpected IOException. The program stops.", e);
        }
        System.exit(-1);
    }
}
