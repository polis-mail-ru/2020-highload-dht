package ru.mail.polis.service.stasyanoi.server.helpers;

import one.nio.http.HttpSession;

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
    public Arguments(final String idParam, final HttpSession session) {
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
