package ru.mail.polis.service.gogun;

import one.nio.http.Response;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

final class GetBodyHandler implements HttpResponse.BodyHandler<Response> {

    final static HttpResponse.BodyHandler<Response> INSTANCE = new GetBodyHandler();

    private GetBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Response> apply(HttpResponse.ResponseInfo responseInfo) {
        final Optional<String> timestamp = responseInfo.headers().firstValue("timestamp");
        final Optional<String> isTombstone = responseInfo.headers().firstValue("tombstone");
        switch (responseInfo.statusCode()) {
            case 200:
                if (timestamp.isEmpty()) {
                    throw new IllegalStateException("No timestamp");
                }

                if (isTombstone.isEmpty()) {
                    throw new IllegalStateException("No tombstone");
                }

                return HttpResponse.BodySubscribers.mapping(
                        HttpResponse.BodySubscribers.ofByteArray(),
                        bytes -> {
                            final Response response = Response.ok(bytes);
                            response.addHeader("timestamp: " + timestamp.get());
                            response.addHeader("tombstone: " + isTombstone.get());
                            return response;
                        }
                );
            case 404:
                Response response = new Response(Response.NOT_FOUND, Response.EMPTY);
                if (timestamp.isPresent()) {
                    response.addHeader("tombstone: " + true);
                }
                return HttpResponse.BodySubscribers.replacing(response);
            default:
                throw new RejectedExecutionException("cant");
        }
    }
}
