package ru.mail.polis.service.s3ponia;

import java.io.IOException;

@FunctionalInterface
public interface ProxyHandler {
    void apply() throws IOException;
}
