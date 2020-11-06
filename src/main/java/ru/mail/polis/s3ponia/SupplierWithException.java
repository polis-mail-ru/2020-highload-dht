package ru.mail.polis.s3ponia;

import java.io.IOException;

@FunctionalInterface
public interface SupplierWithException<T> {
    T get() throws IOException;
}
