package ru.mail.polis.service.manikhin.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.manikhin.TimestampRecord;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.OptionalLong;
import java.util.concurrent.RejectedExecutionException;

public final class GetBodyHandler implements HttpResponse.BodyHandler<TimestampRecord> {

    public static final HttpResponse.BodyHandler<TimestampRecord> INSTANCE = new GetBodyHandler();
    private static final String TIMESTAMP_HEADER = "Timestamp";

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<TimestampRecord> apply(@NotNull final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case 200:
                final OptionalLong successTimestamp = responseInfo.headers().firstValueAsLong(TIMESTAMP_HEADER);

                if (successTimestamp.isEmpty()) {
                    throw new IllegalArgumentException("No timestamp header");
                }

                return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> TimestampRecord.fromValue(ByteBuffer.wrap(bytes), successTimestamp.getAsLong()));
            case 404:
                final OptionalLong errorTimestamp = responseInfo.headers().firstValueAsLong(TIMESTAMP_HEADER);

                if (errorTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(TimestampRecord.getEmpty());
                }

                return HttpResponse.BodySubscribers.replacing(TimestampRecord.tombstone(errorTimestamp.getAsLong()));
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}
