package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpSession;
import one.nio.http.Request;

import java.nio.ByteBuffer;

/**
 * Utility class to use less parameters.
 * */
class Context {
    private final boolean isProxy;
    private final HttpSession session;
    private final Request request;
    private final Replicas replicaFactor;
    private ByteBuffer rangeStart;
    private ByteBuffer rangeEnd;
    private StreamingSession streamingSession;


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

    public ByteBuffer getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(ByteBuffer rangeStart) {
        this.rangeStart = rangeStart;
    }

    public ByteBuffer getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(ByteBuffer rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public StreamingSession getStreamingSession() {
        return streamingSession;
    }

    public void setStreamingSession(StreamingSession streamingSession) {
        this.streamingSession = streamingSession;
    }
}
