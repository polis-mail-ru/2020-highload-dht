package ru.mail.polis.util;

import ru.mail.polis.service.s3ponia.DaoOperationException;

import java.io.IOException;

@FunctionalInterface
public interface SupplierWithException<T> {
    T get() throws IOException, DaoOperationException;
}
