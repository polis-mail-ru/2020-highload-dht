package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpSession;

import java.io.IOException;

public interface HttpEntitiesHandler {
    void entities(final String start,
                  final String end,
                  final StreamingSession session) throws IOException;
}
