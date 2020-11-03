package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpSession;
import one.nio.http.Request;

/**
 * Utility class to use less parameters.
 * */
class Context {
    private final boolean isProxy;
    private final HttpSession session;
    private final Request request;
    private final Replicas replicaFactor;

    public Context(final HttpSession session,
                   final boolean isProxy,
                   final Request request,
                   final Replicas replicaFactor) {
        this.session = session;
        this.isProxy = isProxy;
        this.request = request;
        this.replicaFactor = replicaFactor;
    }

    public boolean isProxy() {
        return isProxy;
    }

    public HttpSession getSession() {
        return session;
    }

    public Request getRequest() {
        return request;
    }

    public Replicas getReplicaFactor() {
        return replicaFactor;
    }
}
