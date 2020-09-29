package ru.mail.polis.dao.stasyanoi;

import com.google.common.primitives.UnsignedBytes;
import org.rocksdb.AbstractComparator;
import org.rocksdb.ComparatorOptions;
import ru.mail.polis.service.stasyanoi.CustomServer;

import java.nio.ByteBuffer;

public class ComparatorImpl extends AbstractComparator {
    java.util.Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();

    public ComparatorImpl(final ComparatorOptions copt) {
        super(copt);
    }

    @Override
    public String name() {
        return "lex";
    }

    @Override
    public int compare(ByteBuffer a, ByteBuffer b) {
        return comparator.compare(CustomServer.toBytes(a), CustomServer.toBytes(b));
    }
}
