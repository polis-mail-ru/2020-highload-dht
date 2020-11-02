package ru.mail.polis.service.zvladn7;

import com.google.common.primitives.Longs;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class ResponseValue {

    private static final int NO_TIMESTAMP_VALUE = -1;

    private final long timestamp;
    @NotNull
    private final byte[] body;
    @NotNull
    private final State state;

    private ResponseValue(final long timestamp,
                          @NotNull final byte[] body,
                          @NotNull final State state) {
        this.timestamp = timestamp;
        this.body = body.clone();
        this.state = state;
    }

    long getTimestamp() {
        return timestamp;
    }

    public static ResponseValue active(final long timestamp, @NotNull final byte[] body) {
        return new ResponseValue(timestamp, body, State.ACTIVE);
    }

    public static ResponseValue deleted(final long timpestamp) {
        return new ResponseValue(timpestamp, Response.EMPTY, State.DELETED);
    }

    public static ResponseValue absent() {
        return new ResponseValue(NO_TIMESTAMP_VALUE, Response.EMPTY, State.ABSENT);
    }

    static Response toProxyResponse(@NotNull final ResponseValue value) {
        return toResponse(value, ResponseValue::proxyResponse);
    }

    static Response toResponse(@NotNull final ResponseValue value) {
        return toResponse(value, v -> Response.ok(v.body));
    }

    private static Response toResponse(@NotNull final ResponseValue value,
                                       @NotNull final Function<ResponseValue, Response> responseProvider) {
        switch (value.state) {
            case ACTIVE:
                return responseProvider.apply(value);
            case DELETED:
                return new Response(Response.NOT_FOUND, Longs.toByteArray(value.timestamp));
            case ABSENT:
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            default:
                throw new IllegalStateException("Unknown value response value state");
        }
    }

    private static Response proxyResponse(@NotNull final ResponseValue value) {
        final byte[] responesBody = new byte[value.body.length + Long.BYTES];
        System.arraycopy(value.body, 0, responesBody, 0, value.body.length);
        System.arraycopy(
                Longs.toByteArray(value.timestamp), 0, responesBody, value.body.length, Long.BYTES);
        return Response.ok(responesBody);
    }

    enum State {
        ACTIVE,
        DELETED,
        ABSENT
    }
}
