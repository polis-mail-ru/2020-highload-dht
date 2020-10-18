package ru.mail.polis.service.mrsandman5.clustering;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@FunctionalInterface
public interface ResponseSupplier {
    @NotNull
    Response supply() throws IOException;
}