package ru.mail.polis.dao.Basta123;

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
        int comparisonResult = SignedBytes.lexicographicalComparator().compare(a.data(), b.data());
        return comparisonResult;
    }


}
