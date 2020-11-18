package ru.mail.polis.service.gogun;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

final class GetBodyHandler implements HttpResponse.BodyHandler<Entry> {

    static final HttpResponse.BodyHandler<Entry> INSTANCE = new GetBodyHandler();

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Entry> apply(final HttpResponse.ResponseInfo responseInfo) {
        final Optional<String> timestamp = responseInfo.headers().firstValue("timestamp");
        if (timestamp.isEmpty()) {
            throw new IllegalStateException("No timestamp");
        }
        switch (responseInfo.statusCode()) {
            case 200:
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> new Entry(Long.parseLong(timestamp.get()), bytes, Entry.OK)
                );
            case 404:
                final Entry entry = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Entry.NOT_FOUND);
                timestamp.ifPresent(time -> entry.setTimestamp(Long.parseLong(time)));
                return HttpResponse.BodySubscribers.replacing(entry);
            default:
                throw new RejectedExecutionException("Incorrect status code");
        }
    }
}
