package ru.mail.polis.service.gogun;

import one.nio.http.Response;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

final class PutDeleteBodyHandler implements HttpResponse.BodyHandler<Response> {

    static final HttpResponse.BodyHandler<Response> INSTANCE = new PutDeleteBodyHandler();

    private PutDeleteBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Response> apply(final HttpResponse.ResponseInfo responseInfo) {
        Response response;
        switch (responseInfo.statusCode()) {
            case 201:
                response = new Response(Response.CREATED, Response.EMPTY);
                return HttpResponse.BodySubscribers.replacing(response);
            case 202:
                response = new Response(Response.ACCEPTED, Response.EMPTY);
                return HttpResponse.BodySubscribers.replacing(response);
            case 500:
                response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                return HttpResponse.BodySubscribers.replacing(response);
            default:
                throw new RejectedExecutionException("cant");
        }
    }
}
