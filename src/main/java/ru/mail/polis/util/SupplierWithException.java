package ru.mail.polis.util;

import java.io.IOException;

@FunctionalInterface
public interface SupplierWithException<T> {
    T get() throws IOException;
}
