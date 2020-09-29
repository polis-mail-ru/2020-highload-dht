package ru.mail.polis.dao.Mariarheon;

import java.io.IOException;

public class DAOException extends IOException {
    private static final long serialVersionUID = 1000L;

    public DAOException (final String message, final Throwable cause) {
        super(message, cause);
    }
}
