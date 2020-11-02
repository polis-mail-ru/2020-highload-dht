package ru.mail.polis.service.zvladn7.bodyhandlers;

import com.google.common.primitives.Longs;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.zvladn7.ResponseValue;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

public final class GetBodyHandler implements HttpResponse.BodyHandler<ResponseValue> {

    public static final HttpResponse.BodyHandler<ResponseValue> INSTANCE = new GetBodyHandler();
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<ResponseValue> apply(final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case OK:
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        body -> ResponseValue.active(getTimestamp(body), getValue(body)));
            case NOT_FOUND:
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        body -> {
                            if (body.length == 0) {
                                return ResponseValue.absent();
                            } else {
                                return ResponseValue.deleted(getTimestamp(body));
                            }
                        });
            default:
                throw new RejectedExecutionException("Cannot process GET response");
        }
    }

    private static byte[] getValue(@NotNull final byte[] body) {
        final byte[] valueBody = new byte[body.length - Long.BYTES];
        System.arraycopy(body, 0, valueBody, 0, valueBody.length);
        return valueBody;
    }

    private static long getTimestamp(@NotNull final byte[] body) {
        final byte[] timestampBytes = new byte[Long.BYTES];
        System.arraycopy(body, body.length - Long.BYTES, timestampBytes, 0, Long.BYTES);
        return Longs.fromByteArray(timestampBytes);
    }
}
