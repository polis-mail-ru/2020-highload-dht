package ru.mail.polis.dao.asta1234;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.Comparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;

public class MyComparator extends Comparator {

    public MyComparator(final ComparatorOptions compOptions) {
        super(compOptions);
    }

    @Override
    public String name() {
        return "something";
    }

    @Override
    public int compare(final Slice a, final Slice b) {
        return SignedBytes.lexicographicalComparator().compare(a.data(), b.data());
    }

}
