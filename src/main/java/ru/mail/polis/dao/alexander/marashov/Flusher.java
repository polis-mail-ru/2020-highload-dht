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

    private final Logger log = LoggerFactory.getLogger(Flusher.class);

    final BlockingQueue<NumberedTable> tablesQueue;

    private final String suffix;
    private final String temp;
    private final File storage;
    private final BiConsumer<Integer, File> tableFlashedCallback;

    /**
     * Flusher constructor.
     * @param storage - root of the DAO storage.
     * @param capacity - capacity of the BlockingQueue, max count of the Table-to-flush tasks.
     * @param tableFlushedCallback - callback function which called after table flushing.
     * @param suffix - data file extension
     * @param temp - temporary data file extension
     */
    public Flusher(
            final File storage,
            final int capacity,
            final BiConsumer<Integer, File> tableFlushedCallback,
            final String suffix,
            final String temp
    ) {
        super("Flusher");
        setDaemon(true);
        this.storage = storage;
        this.tablesQueue = new ArrayBlockingQueue<>(capacity);
        this.tableFlashedCallback = tableFlushedCallback;
        this.suffix = suffix;
        this.temp = temp;
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
                final File file = new File(this.storage, generation + temp);
                SSTable.serialize(cellIterator, file);
                final File dst = new File(this.storage, generation + suffix);
                Files.move(file.toPath(), dst.toPath(), StandardCopyOption.ATOMIC_MOVE);
                this.tableFlashedCallback.accept(numberedTable.generation, dst);
            }
        } catch (final InterruptedException e) {
            log.error("Flusher interrupted. The program stops.", e);
        } catch (final IOException e) {
            log.error("Flusher met an unexpected IOException. The program stops.", e);
        }
        System.exit(-1);
    }
}
