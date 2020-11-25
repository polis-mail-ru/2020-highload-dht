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

        switch (responseInfo.statusCode()) {
            case 200:
                if (timestamp.isEmpty()) {
                    throw new IllegalStateException("No timestamp");
                }
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> Entry.present(Long.parseLong(timestamp.get()), bytes)
                );
            case 404:
                Entry entry = Entry.absent();
                if (timestamp.isPresent()) {
                    entry = Entry.removed(Long.parseLong(timestamp.get()));
                }

                return HttpResponse.BodySubscribers.replacing(entry);
            default:
                throw new RejectedExecutionException("Incorrect status code");
        }
    }
}
