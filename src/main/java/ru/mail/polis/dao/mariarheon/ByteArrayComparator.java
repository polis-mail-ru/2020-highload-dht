package ru.mail.polis.dao.mariarheon;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.AbstractComparator;
import org.rocksdb.ComparatorOptions;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class ByteArrayComparator extends AbstractComparator {
    private final Comparator<byte[]> comparator;

    public ByteArrayComparator() {
        super(new ComparatorOptions());
        comparator = SignedBytes.lexicographicalComparator();
    }

    @Override
    public String name() {
        return "Byte array comparator";
    }

    @Override
    public int compare(ByteBuffer a, ByteBuffer b) {
        var aBytes = new byte[a.remaining()];
        var bBytes = new byte[b.remaining()];
        a.get(aBytes);
        b.get(bBytes);
        return comparator.compare(aBytes, bBytes);
    }
}
