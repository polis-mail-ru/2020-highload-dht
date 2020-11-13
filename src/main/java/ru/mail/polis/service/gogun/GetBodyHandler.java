package ru.mail.polis.service.gogun;

import one.nio.http.Response;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

final class GetBodyHandler implements HttpResponse.BodyHandler<Response> {

    static final HttpResponse.BodyHandler<Response> INSTANCE = new GetBodyHandler();

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Response> apply(final HttpResponse.ResponseInfo responseInfo) {
        final Optional<String> timestamp = responseInfo.headers().firstValue("timestamp");
        if (timestamp.isEmpty()) {
            throw new IllegalStateException("No timestamp");
        }
        switch (responseInfo.statusCode()) {
            case 200:
                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> {
                            final Response response = Response.ok(bytes);
                            response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + timestamp.get());
                            return response;
                        }
                );
            case 404:
                final Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                timestamp.ifPresent(time -> response.addHeader(AsyncServiceImpl.TIMESTAMP_HEADER + time));
                return HttpResponse.BodySubscribers.replacing(response);
            default:
                throw new RejectedExecutionException("Incorrect status code");
        }
    }
}
