package ru.mail.polis.service.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.Value;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.OptionalLong;
import java.util.concurrent.RejectedExecutionException;

public final class GetBodyHandler implements HttpResponse.BodyHandler<Value> {
    private static final String TIMESTAMP = "Timestamp";

    public static final HttpResponse.BodyHandler<Value> INSTANCE = new GetBodyHandler();

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Value> apply(
            @NotNull final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case 200:
                final OptionalLong okTimestamp =
                        responseInfo.headers().firstValueAsLong(TIMESTAMP);
                if (okTimestamp.isEmpty()) {
                    throw new IllegalArgumentException("No timestamp header");
                }
                final long okTimestampValue = okTimestamp.getAsLong();
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> Value.resolveExistingValue(ByteBuffer.wrap(bytes), okTimestampValue));
            case 404:
                final OptionalLong notFoundTimestamp =
                        responseInfo.headers().firstValueAsLong(TIMESTAMP);
                if (notFoundTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(
                            Value.resolveMissingValue());
                }
                final long notFoundTimestampValue = notFoundTimestamp.getAsLong();
                return HttpResponse.BodySubscribers.replacing(
                        Value.resolveDeletedValue(notFoundTimestampValue));
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}
