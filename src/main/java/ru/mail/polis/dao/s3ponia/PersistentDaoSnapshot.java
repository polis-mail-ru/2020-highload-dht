package ru.mail.polis.dao.s3ponia;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DaoSnapshot;
import ru.mail.polis.util.hash.ConcatHash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;

import static ru.mail.polis.util.Utility.fromByteArray;

public class PersistentDaoSnapshot implements DaoSnapshot {
    private final TableSet tableSet;
    private final ConcatHash hash;

    private byte[] hash(@NotNull final ICell cell) {
        final var byteBuffer =
                ByteBuffer
                        .allocate(
                                cell.getKey().capacity()
                                        + cell.getValue().getValue().capacity()
                                        + Long.BYTES /* TIMESTAMP */
                        );
        byteBuffer.put(cell.getKey());
        byteBuffer.put(cell.getValue().getValue());
        byteBuffer.asLongBuffer().put(cell.getValue().getDeadFlagTimeStamp());
        return hash.hash(byteBuffer.array());
    }

    private long longHash(@NotNull final byte[] hashArray) {
        return fromByteArray(hashArray, 0, Long.BYTES);
    }

    private Iterator<ICell> iterator() {
        return tableSet.cellsIterator(ByteBuffer.allocate(0));
    }

    /**
     * Constructs a {@link PersistentDaoSnapshot} with given table set and hash.
     *
     * @param tableSet snapshot's table set
     * @param hash     hash
     */
    public PersistentDaoSnapshot(@NotNull final TableSet tableSet, @NotNull final ConcatHash hash) {
        this.tableSet = tableSet;
        this.hash = hash;
    }

    @Override
    public Iterator<ICell> range(final long start, final long end) {
        return Iterators.filter(iterator(), c -> {
            final var hash = longHash(hash(c));
            return hash >= start && hash <= end;
        });
    }

    @Override
    public byte[] hash(final long start, final long end) {
        final var iterator = range(start, end);
        var accumulateValue = new byte[hash.hashSize()];

        while (iterator.hasNext()) {
            accumulateValue = hash.combine(accumulateValue, hash(iterator.next()));
        }

        return accumulateValue;
    }

    @Override
    public void saveTo(@NotNull final Path path) throws IOException {
        DiskManager.saveTo(iterator(), path);
    }

    @Override
    public void saveTo(@NotNull final Path path, final long start, final long end) throws IOException {
        DiskManager.saveTo(range(start, end), path);
    }
}
