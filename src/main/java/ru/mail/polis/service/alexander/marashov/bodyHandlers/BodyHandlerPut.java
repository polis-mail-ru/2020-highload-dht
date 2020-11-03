package ru.mail.polis.service.alexander.marashov.bodyHandlers;

import one.nio.http.Response;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

public class BodyHandlerPut implements HttpResponse.BodyHandler<byte[]> {
    public static final BodyHandlerPut INSTANCE = new BodyHandlerPut();

    private BodyHandlerPut() {

    }

    @Override
    public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo responseInfo) {
        if (responseInfo.statusCode() == 201) {
            return HttpResponse.BodySubscribers.replacing(Response.EMPTY);
        }
        throw new RejectedExecutionException("Can't get response");
    }
}
