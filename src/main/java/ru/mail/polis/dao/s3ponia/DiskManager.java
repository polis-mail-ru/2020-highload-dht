package ru.mail.polis.dao.s3ponia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.s3ponia.DaoOperationException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DiskManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(DiskManager.class);
    static final String META_EXTENSION = ".mdb";
    static final String META_PREFIX = "fzxyGZ9LDM";
    private final Path metaFile;
    private static final String TABLE_EXTENSION = ".db";
    private static final String UNIQUE_EXTENSION = ".tdb";

    private final List<String> fileNames;
    private final List<DiskTable> diskTables;
    private int generation;

    /**
     * Saves {@link ICell}s from iterator to given file.
     * @param it iterator over saving {@link ICell}s
     * @param file file to save
     * @throws IOException rethrow from {@link FileChannel#write}
     */
    public static void saveTo(final Iterator<ICell> it, final Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        try (FileChannel writer = FileChannel.open(file, StandardOpenOption.WRITE)) {
            final var shifts = new ArrayList<Integer>();
            shifts.add(0);
            var index = 0;
            while (it.hasNext()) {
                final var cell = it.next();
                int nextShift = shifts.get(index);
                final var key = cell.getKey();
                final var value = cell.getValue();

                nextShift += key.remaining() + value.getValue().remaining() + Long.BYTES /* Meta size */
                        + Integer.BYTES /* Shift size */;

                writer.write(ByteBuffer.allocate(Long.BYTES).putLong(value.getDeadFlagTimeStamp()).flip());
                writer.write(ByteBuffer.allocate(Integer.BYTES).putInt(key.remaining()).flip());
                writer.write(key);
                writer.write(value.getValue());

                shifts.add(nextShift);
                ++index;
            }

            shifts.remove(index);

            for (final Integer o : shifts) {
                writer.write(ByteBuffer.allocate(Integer.BYTES).putInt(o).flip());
            }
            writer.write(ByteBuffer.allocate(Integer.BYTES).putInt(shifts.size()).flip());
        }
    }

    private void setSeed() {
        if (fileNames.isEmpty()) {
            return;
        }
        for (final var file : fileNames) {
            final var fileName = Paths.get(file).getFileName().toString();
            final var fileGen = fileName.substring(0, fileName.length() - 3);
            generation = Math.max(Integer.parseInt(fileGen), generation);
        }
        generation += 1;
    }

    private String getName(final int daoGeneration) {
        return Integer.toString(daoGeneration);
    }

    DiskManager(final Path file) throws IOException {
        metaFile = file;
        if (!Files.exists(metaFile)) {
            Files.createFile(metaFile);
        }

        fileNames = Files.readAllLines(metaFile);
        final List<DiskTable> list = new ArrayList<>();
        for (final String fileName : fileNames) {
            final Path path = Paths.get(fileName);
            final DiskTable of;
            try {
                of = DiskTable.of(path);
                list.add(of);
            } catch (DaoOperationException e) {
                logger.error("Error in DiskTable's creation", e);
            }
        }
        diskTables = list;
        setSeed();
    }

    List<DiskTable> diskTables() {
        return diskTables;
    }

    synchronized void clear() throws IOException {
        Files.delete(metaFile);
        Files.createFile(metaFile);
    }

    DiskTable diskTableFromGeneration(final int generation) throws DaoOperationException {
        final var filePath = Paths.get(metaFile.getParent().toString(), getName(generation) + TABLE_EXTENSION);
        return DiskTable.of(filePath);
    }

    private void saveFileNameToMeta(final String fileName) throws IOException {
        try (var writer = Files.newBufferedWriter(this.metaFile,
                Charset.defaultCharset(), StandardOpenOption.APPEND)) {
            writer.write(fileName + "\n");
        }
    }
    
    /**
     * Saves {@link ICell}s in given {@code Iterator<ICell>} on disk.
     * @param it {@code Iterator<ICell>}
     * @param generation {@link ICell}s' generation.
     * @throws IOException rethrows from
     */
    public void save(final Iterator<ICell> it, final int generation) throws IOException {
        final var filePath = Paths.get(metaFile.getParent().toString(), getName(generation) + TABLE_EXTENSION);
        final var fileName = filePath.toString();
        saveFileNameToMeta(fileName);
        saveTo(it, filePath);
    }
    
    /**
     * Creates file from given generation.
     * @param generation generation
     * @return created file
     * @throws IOException rethrow from {@link Files#createFile}
     */
    public Path uniqueFile(final long generation) throws IOException {
        final var path = Paths.get(metaFile.getParent().toAbsolutePath().toString(), generation + UNIQUE_EXTENSION);
        Files.createFile(path);
        return path;
    }

    int getGeneration() {
        return generation;
    }

    @Override
    public void close() throws IOException {
        diskTables.clear();
        fileNames.clear();
    }
}
