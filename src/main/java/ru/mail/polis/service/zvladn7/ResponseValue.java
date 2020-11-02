package ru.mail.polis.service.zvladn7;

import com.google.common.primitives.Longs;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class ResponseValue {

    private static final int NO_TIMESTAMP_VALUE = -1;

    private long timpestamp;
    @NotNull
    private byte[] body;
    @NotNull
    private State state;

    private ResponseValue(final long timpestamp,
                          @NotNull final byte[] body,
                          @NotNull final State state) {
        this.timpestamp = timpestamp;
        this.body = body;
        this.state = state;
    }

    public long getTimpestamp() {
        return timpestamp;
    }

    @NotNull
    public byte[] getBody() {
        return body;
    }

    @NotNull
    public State getState() {
        return state;
    }

    public static ResponseValue active(final long timestamp, final byte[] body) {
        return new ResponseValue(timestamp, body, State.ACTIVE);
    }

    public static ResponseValue deleted(final long timpestamp) {
        return new ResponseValue(timpestamp, Response.EMPTY, State.DELETED);
    }

    public static ResponseValue absent() {
        return new ResponseValue(NO_TIMESTAMP_VALUE, Response.EMPTY, State.ABSENT);
    }

    static Response toProxyResponse(final ResponseValue value) {
        return toResponse(value, ResponseValue::proxyResponse);
    }

    static Response toResponse(final ResponseValue value) {
        return toResponse(value, v -> Response.ok(v.body));
    }

    private static Response toResponse(final ResponseValue value, final Function<ResponseValue, Response> responseProvider) {
        switch (value.state) {
            case ACTIVE:
                return responseProvider.apply(value);
            case DELETED:
                return new Response(Response.NOT_FOUND, Longs.toByteArray(value.timpestamp));
            case ABSENT:
                return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        throw new IllegalStateException("Unknown value response value state");
    }

    private static Response proxyResponse(final ResponseValue value) {
        final byte[] responesBody = new byte[value.body.length + Long.BYTES];
        System.arraycopy(value.body, 0, responesBody, 0, value.body.length);
        System.arraycopy(
                Longs.toByteArray(value.timpestamp), 0, responesBody, value.body.length, Long.BYTES);
        return Response.ok(responesBody);
    }

    enum State {
        ACTIVE,
        DELETED,
        ABSENT
    }
}
