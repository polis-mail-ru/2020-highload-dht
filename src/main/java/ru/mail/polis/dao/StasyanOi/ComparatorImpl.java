package ru.mail.polis.dao.StasyanOi;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.Comparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;

public class ComparatorImpl extends Comparator {
    public ComparatorImpl(ComparatorOptions copt) {
        super(copt);
    }

    java.util.Comparator<byte[]> comparator = SignedBytes.lexicographicalComparator();

    @Override
    public String name() {
        return "lex";
    }

    @Override
    public int compare(Slice a, Slice b) {
        return comparator.compare(a.data(), b.data());
    }
}
