package ru.mail.polis.util.hash;

import gnu.crypto.hash.Tiger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class TigerHash implements StreamHash, ConcatHash {
    private final Tiger tiger = new Tiger();
    private final int blockSize = tiger.hashSize();
    
    @Override
    public byte[] combine(@NotNull final byte[] src, final int offset1, final int offset2) {
        if (src.length - offset1 < blockSize || src.length - offset2 < blockSize
                    || Math.abs(offset1 - offset2) < blockSize) {
            throw new IllegalArgumentException("Small hashes' size");
        }
        
        tiger.reset();
        tiger.update((byte) 0x01);
        tiger.update(src, offset1, blockSize);
        tiger.update(src, offset2, blockSize);
        return tiger.digest();
    }
    
    @Override
    public byte[] combine(@NotNull final byte[] a, @NotNull final byte[] b) {
        if (a.length != blockSize || b.length != blockSize) {
            throw new IllegalArgumentException("Illegal hashes' size");
        }
        tiger.reset();
        tiger.update((byte) 0x01);
        tiger.update(a, 0, blockSize);
        tiger.update(b, 0, blockSize);
        return tiger.digest();
    }
    
    @Override
    public byte[] hash(@NotNull final InputStream stream) throws IOException {
        final byte[] processingBytes = new byte[blockSize];
        int read;
        do {
            read = stream.read(processingBytes);
            tiger.update(processingBytes, 0, read);
        } while (read > 0);
        return tiger.digest();
    }
    
    @Override
    public byte[] hash(@NotNull final byte[] in) {
        tiger.reset();
        tiger.update((byte) 0x00);
        tiger.update(in, 0, in.length);
        return tiger.digest();
    }
    
    @Override
    public int hashSize() {
        return blockSize;
    }
}
