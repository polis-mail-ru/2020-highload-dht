package ru.mail.polis.util.hash;

import gnu.crypto.hash.Tiger;

import java.io.IOException;
import java.io.InputStream;

public interface StreamHash extends Hash {
    public byte[] hash(InputStream stream) throws IOException;
}
