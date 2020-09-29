package ru.mail.polis.util;

import com.google.common.primitives.SignedBytes;
import org.rocksdb.AbstractComparator;
import org.rocksdb.ComparatorOptions;

import java.nio.ByteBuffer;

public class SignedBytesComparator extends AbstractComparator {

    public SignedBytesComparator(final ComparatorOptions copt) {
        super(copt);
    }
    @Override
    public String name() {
        return "ru.mail.polis.util.SignedBytesComparator";
    }

    @Override
    public int compare(final ByteBuffer a, final ByteBuffer b) {
        return SignedBytes.lexicographicalComparator()
                .compare(Util.byteBufferToBytes(a),Util.byteBufferToBytes(b));
    }
}
