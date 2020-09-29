package ru.mail.polis.dao.stasyanoi;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.Comparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;

public class ComparatorImpl extends Comparator {
    java.util.Comparator<byte[]> comparator = SignedBytes.lexicographicalComparator();

    public ComparatorImpl(final ComparatorOptions copt) {
        super(copt);
    }

    @Override
    public String name() {
        return "lex";
    }

    @Override
    public int compare(Slice a, Slice b) {
        return comparator.compare(a.data(), b.data());
    }
}
