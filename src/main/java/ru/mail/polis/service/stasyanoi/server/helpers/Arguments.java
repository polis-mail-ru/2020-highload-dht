package ru.mail.polis.service.stasyanoi.server.helpers;

import one.nio.http.HttpSession;
import one.nio.http.Request;

/**
 * Arguments for requests.
 */
public class Arguments {

    private final String idParam;
    private final HttpSession session;

    /**
     * Arguments for requests.
     *
     * @param idParam - key.
     * @param session - session.
     */
    public Arguments(String idParam, HttpSession session) {
        this.idParam = idParam;
        this.session = session;
    }

    public HttpSession getSession() {
        return session;
    }

    public String getIdParam() {
        return idParam;
    }
}
