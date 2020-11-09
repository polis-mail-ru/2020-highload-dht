package ru.mail.polis.service.mrsandman5.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.mrsandman5.replication.Entry;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
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
                final Optional<String> okTimestamp =
                        responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
                if (okTimestamp.isEmpty()) {
                    throw new IllegalArgumentException("No timestamp header");
                }
                final String ofTimestampValue = okTimestamp.get();
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> Entry.present(Long.parseLong(Objects.requireNonNull(ofTimestampValue)), bytes));
            case 404:
                final Optional<String> notFoundTimestamp =
                        responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
                if (notFoundTimestamp.isEmpty()) {
                    return HttpResponse.BodySubscribers.replacing(
                            Entry.absent());
                }
                final String notFoundTimestampValue = notFoundTimestamp.get();
                return HttpResponse.BodySubscribers.replacing(
                        Entry.removed(Long.parseLong(notFoundTimestampValue)));
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}
