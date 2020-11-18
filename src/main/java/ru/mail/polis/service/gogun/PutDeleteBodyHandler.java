package ru.mail.polis.service.gogun;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

final class PutDeleteBodyHandler implements HttpResponse.BodyHandler<Entry> {

    static final HttpResponse.BodyHandler<Entry> INSTANCE = new PutDeleteBodyHandler();

    private PutDeleteBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Entry> apply(final HttpResponse.ResponseInfo responseInfo) {
        Entry entry;
        switch (responseInfo.statusCode()) {
            case 201:
                entry = new Entry(Entry.CREATED);
                return HttpResponse.BodySubscribers.replacing(entry);
            case 202:
                entry = new Entry(Entry.ACCEPTED);
                return HttpResponse.BodySubscribers.replacing(entry);
            case 500:
                entry = new Entry(Entry.INTERNAL_ERROR);
                return HttpResponse.BodySubscribers.replacing(entry);
            default:
                throw new RejectedExecutionException("cant");
        }
    }
}
