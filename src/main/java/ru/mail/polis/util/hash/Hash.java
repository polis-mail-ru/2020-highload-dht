package ru.mail.polis.util.hash;

public interface Hash {
    byte[] hash(final byte[] in);

    int hashSize();
}
