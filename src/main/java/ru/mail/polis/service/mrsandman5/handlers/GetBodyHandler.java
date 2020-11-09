package ru.mail.polis.service.mrsandman5.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.RejectedExecutionException;

public final class GetBodyHandler implements HttpResponse.BodyHandler<Entry> {

    public static final HttpResponse.BodyHandler<Entry> INSTANCE = new GetBodyHandler();

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Entry> apply(
            @NotNull final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case 200:
                final OptionalLong okTimestamp =
                        responseInfo.headers().firstValueAsLong(ResponseUtils.TIMESTAMP);
                if (okTimestamp.isEmpty()) {
                    throw new IllegalArgumentException("No timestamp header");
                }
                final long okTimestampValue = okTimestamp.getAsLong();
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> Entry.present(okTimestampValue, bytes));
            case 404:
                final OptionalLong notFoundTimestamp =
                        responseInfo.headers().firstValueAsLong(ResponseUtils.TIMESTAMP);
                if (notFoundTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(
                            Entry.absent());
                }
                final long notFoundTimestampValue = notFoundTimestamp.getAsLong();
                return HttpResponse.BodySubscribers.replacing(
                        Entry.removed(notFoundTimestampValue));
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}
