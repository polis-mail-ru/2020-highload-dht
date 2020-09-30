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
    public int compare(final ByteBuffer a, final ByteBuffer b) {
        final var fs = new byte[a.remaining()];
        final var sc = new byte[b.remaining()];
        a.get(fs);
        b.get(sc);
        return comparator.compare(fs, sc);
    }
}
