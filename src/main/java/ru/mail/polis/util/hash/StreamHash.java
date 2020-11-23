package ru.mail.polis.util.hash;

import java.io.IOException;
import java.io.InputStream;

public interface StreamHash extends Hash {
    byte[] hash(InputStream stream) throws IOException;
}
