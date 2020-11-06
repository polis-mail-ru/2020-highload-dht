package ru.mail.polis.s3ponia;

import java.io.IOException;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws IOException;
}
