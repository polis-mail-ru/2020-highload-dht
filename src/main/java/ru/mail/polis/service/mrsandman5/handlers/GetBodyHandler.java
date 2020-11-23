package ru.mail.polis.service.mrsandman5.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.time.Instant;
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
                final Optional<String> ofExpires =
                        responseInfo.headers().firstValue(ResponseUtils.EXPIRES);
                final Instant okExpireTime =
                        ofExpires.isEmpty() ? null : ResponseUtils.parseExpires(ofExpires.get());
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> Entry.present(okTimestampValue, bytes, okExpireTime));
            case 404:
                final OptionalLong notFoundTimestamp =
                        responseInfo.headers().firstValueAsLong(ResponseUtils.TIMESTAMP);
                if (notFoundTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(
                            Entry.absent());
                }
                final long notFoundTimestampValue = notFoundTimestamp.getAsLong();
                final Optional<String> notFoundExpires =
                        responseInfo.headers().firstValue(ResponseUtils.EXPIRES);
                final Instant notFoundExpireTime =
                        notFoundExpires.isEmpty() ? null : ResponseUtils.parseExpires(notFoundExpires.get());
                return HttpResponse.BodySubscribers.replacing(
                        Entry.removed(notFoundTimestampValue, notFoundExpireTime));
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}
