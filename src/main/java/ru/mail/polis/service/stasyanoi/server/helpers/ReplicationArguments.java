package ru.mail.polis.service.stasyanoi.server.helpers;

import one.nio.http.HttpSession;
import one.nio.http.Request;

public class ReplicationArguments {

    private final String idParam;
    private final Request request;
    private final HttpSession session;

    public ReplicationArguments(String idParam, HttpSession session) {
        this.idParam = idParam;
        this.session = session;
        this.request = null;
    }

    public ReplicationArguments(String idParam, Request request, HttpSession session) {
        this.idParam = idParam;
        this.request = request;
        this.session = session;
    }

    public HttpSession getSession() {
        return session;
    }

    public Request getRequest() {
        return request;
    }

    public String getIdParam() {
        return idParam;
    }
}
