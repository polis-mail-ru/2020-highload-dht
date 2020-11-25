package ru.mail.polis.util.hash;

import gnu.crypto.hash.Tiger;

import java.io.IOException;
import java.io.InputStream;

public class TigerHash implements StreamHash, ConcatHash {
    private static final int BLOCK_SIZE = 64;
    private final Tiger tiger = new Tiger();

    @Override
    public byte[] combine(final byte[] src, final int offset1, final int offset2) {
        if (src.length - offset1 < BLOCK_SIZE || src.length - offset2 < BLOCK_SIZE
                || Math.abs(offset1 - offset2) < BLOCK_SIZE) {
            throw new IllegalArgumentException("Small hashes' size");
        }

        tiger.reset();
        tiger.update((byte) 0x01);
        tiger.update(src, offset1, BLOCK_SIZE);
        tiger.update(src, offset2, BLOCK_SIZE);
        return tiger.digest();
    }

    @Override
    public byte[] combine(final byte[] a, final byte[] b) {
        if (a.length != BLOCK_SIZE || b.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("Illegal hashes' size");
        }
        tiger.reset();
        tiger.update((byte) 0x01);
        tiger.update(a, 0, BLOCK_SIZE);
        tiger.update(b, 0, BLOCK_SIZE);
        return tiger.digest();
    }

    @Override
    public byte[] hash(InputStream stream) throws IOException {
        final byte[] processingBytes = new byte[BLOCK_SIZE];
        int read;
        do {
            read = stream.read(processingBytes);
            tiger.update(processingBytes, 0, read);
        } while (read > 0);
        return tiger.digest();
    }

    @Override
    public byte[] hash(byte[] in) {
        tiger.reset();
        tiger.update((byte) 0x00);
        tiger.update(in, 0, in.length);
        return tiger.digest();
    }

    @Override
    public int hashSize() {
        return BLOCK_SIZE;
    }
}
