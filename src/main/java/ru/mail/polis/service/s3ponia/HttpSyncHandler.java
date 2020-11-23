package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpSession;
import org.jetbrains.annotations.NotNull;

public interface HttpSyncHandler {
    void sync(@NotNull final HttpSession session);
}
