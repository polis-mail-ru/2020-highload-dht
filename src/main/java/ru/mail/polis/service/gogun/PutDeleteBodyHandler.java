package ru.mail.polis.service.gogun;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

final class PutDeleteBodyHandler implements HttpResponse.BodyHandler<Void> {

    static final HttpResponse.BodyHandler<Void> INSTANCE = new PutDeleteBodyHandler();

    private PutDeleteBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Void> apply(final HttpResponse.ResponseInfo responseInfo) {
        int status = responseInfo.statusCode();
        if (status != 201 && status != 202) {
            throw new RejectedExecutionException("cant");
        }
        return HttpResponse.BodySubscribers.discarding();
    }
}
