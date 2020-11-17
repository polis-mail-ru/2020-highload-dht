/*package ru.mail.polis.dao.basta123;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.Comparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;

public class SingedBytesComparator extends Comparator {

    public SingedBytesComparator(final ComparatorOptions compOptions) {
        super(compOptions);
    }

    @Override
    public String name() {
        return "SingedBytesComparator";
    }

    @Override
    public int compare(final Slice a, final Slice b) {
        return SignedBytes.lexicographicalComparator().compare(a.data(), b.data());
    }

}
*/
