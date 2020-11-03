package ru.mail.polis.service.alexander.marashov.bodyhandlers;

import ru.mail.polis.dao.alexander.marashov.Value;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.OptionalLong;
import java.util.concurrent.RejectedExecutionException;

import static ru.mail.polis.service.alexander.marashov.ServiceImpl.TIMESTAMP_HEADER_NAME;

public final class BodyHandlerGet implements HttpResponse.BodyHandler<Value> {
    public static final BodyHandlerGet INSTANCE = new BodyHandlerGet();

    private BodyHandlerGet() {

    }

    @Override
    public HttpResponse.BodySubscriber<Value> apply(final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case 200:
                final OptionalLong timestamp = responseInfo.headers().firstValueAsLong(TIMESTAMP_HEADER_NAME);
                if (timestamp.isEmpty()) {
                    throw new IllegalArgumentException("No timestamp header");
                }
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> new Value(timestamp.getAsLong(), ByteBuffer.wrap(bytes))
                );
            case 404:
                final OptionalLong tombstoneTimestamp = responseInfo.headers().firstValueAsLong(TIMESTAMP_HEADER_NAME);
                if (tombstoneTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(new Value(0L, null));
                }
                return HttpResponse.BodySubscribers.replacing(
                        new Value(tombstoneTimestamp.getAsLong(), null)
                );
            default:
                throw new RejectedExecutionException("Can't get response");

        }
    }
}
