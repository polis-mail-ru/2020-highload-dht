package ru.mail.polis.util.hash;

public interface ConcatHash extends Hash {

    byte[] combine(final byte[] src, final int offset1, final int offset2);

    byte[] combine(final byte[] a, final byte[] b);
}
