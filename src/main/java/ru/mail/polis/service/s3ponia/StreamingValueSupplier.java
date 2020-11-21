package ru.mail.polis.service.s3ponia;

import java.util.Iterator;

@FunctionalInterface
public interface StreamingValueSupplier<T extends StreamingValue> {
    Iterator<T> get();
}
