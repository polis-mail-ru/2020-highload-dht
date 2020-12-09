package ru.mail.polis.dao.s3ponia;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DaoSnapshot;
import ru.mail.polis.util.MapIterator;
import ru.mail.polis.util.hash.ConcatHash;
import ru.mail.polis.util.hash.TigerHash;
import ru.mail.polis.util.merkletree.MerkleTree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import static ru.mail.polis.util.Utility.fromByteArray;

public class PersistentDaoSnapshot implements DaoSnapshot {
    private final TableSet tableSet;
    private final ConcatHash hash;
    
    private long longHash(@NotNull final byte[] hashArray) {
        return fromByteArray(hashArray, 0, Long.BYTES) & Long.MAX_VALUE;
    }
    
    private Iterator<ICell> iterator() {
        return tableSet.cellsIterator(ByteBuffer.allocate(0));
    }
    
    private static class CachedICellHash {
        final ICell cell;
        final byte[] hash;
        
        CachedICellHash(final ICell cell, final byte[] hash) {
            this.cell = cell;
            this.hash = hash;
        }
    }
    
    private Iterator<CachedICellHash> hashCellRange(final long start, final long end) {
        return Iterators.filter(hashCellIterator(), c -> longHash(c.hash) >= start && longHash(c.hash) <= end);
    }
    
    private Iterator<CachedICellHash> hashCellIterator() {
        return new MapIterator<>(
                tableSet.cellsIterator(ByteBuffer.allocate(0)),
                c -> new CachedICellHash(c, hashCode(c))
        );
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
    public MerkleTree merkleTree(final long blocksCount, final long start, final long end) {
        assert blocksCount > 3;
        assert end >= start;
        final var blocks = new ArrayList<ArrayList<CachedICellHash>>();
        final var step = (end - start) / blocksCount;
        for (int i = 0; i < blocksCount; i++) {
            blocks.add(new ArrayList<>());
        }
    
        hashCellRange(start, end).forEachRemaining(c -> {
            final var hashValue = longHash(c.hash) - start;
            blocks.get((int) (hashValue / step)).add(c);
        });
        
        final var leaves = new ArrayList<byte[]>();
        blocks.forEach(a -> leaves.add(hashCellHashCode(a.iterator())));
        
        return new MerkleTree(leaves, new TigerHash());
    }
    
    @Override
    public Iterator<ICell> range(final long start, final long end) {
        return Iterators.filter(iterator(), c -> {
            final var hashValue = longHash(hashCode(c));
            return hashValue >= start && hashValue <= end;
        });
    }
    
    private byte[] hashCellHashCode(final Iterator<CachedICellHash> iterator) {
        var accumulateValue = new byte[hash.hashSize()];
        
        while (iterator.hasNext()) {
            accumulateValue = hash.combine(accumulateValue, iterator.next().hash);
        }
        
        return accumulateValue;
    }
    
    private byte[] hashCode(@NotNull final ICell cell) {
        final var byteBuffer =
                ByteBuffer
                        .allocate(
                                cell.getKey().capacity()
                                        + cell.getValue().getValue().capacity()
                        );
        byteBuffer.put(cell.getKey());
        byteBuffer.put(cell.getValue().getValue());
        return hash.hash(byteBuffer.array());
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
