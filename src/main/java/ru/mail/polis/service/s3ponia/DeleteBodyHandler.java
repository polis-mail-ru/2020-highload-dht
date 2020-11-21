package ru.mail.polis.service.s3ponia;

import java.net.http.HttpResponse;

public class DeleteBodyHandler implements HttpResponse.BodyHandler<Void> {
    @Override
    public HttpResponse.BodySubscriber<Void> apply(HttpResponse.ResponseInfo responseInfo) {
        if (responseInfo.statusCode() == 202 /* ACCEPTED */) {
            return HttpResponse.BodySubscribers.discarding();
        }
        throw new IllegalArgumentException("Failure in DELETE response");
    }
}
