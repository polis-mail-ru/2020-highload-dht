package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpResponse;

public class UpsertBodyHandler implements HttpResponse.BodyHandler<Void> {
    @Override
    public HttpResponse.BodySubscriber<Void> apply(@NotNull final HttpResponse.ResponseInfo responseInfo) {
        if (responseInfo.statusCode() == 201 /* CREATED */) {
            return HttpResponse.BodySubscribers.discarding();
        }
        throw new IllegalArgumentException("Failure in PUT response");
    }
}
