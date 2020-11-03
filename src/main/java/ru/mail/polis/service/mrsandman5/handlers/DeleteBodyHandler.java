package ru.mail.polis.service.mrsandman5.handlers;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

public class DeleteBodyHandler implements HttpResponse.BodyHandler<String> {

    public static final HttpResponse.BodyHandler<String> INSTANCE = new DeleteBodyHandler();

    private DeleteBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<String> apply(
            @NotNull final HttpResponse.ResponseInfo responseInfo) {
        final int status = responseInfo.statusCode();
        if (status == 202) {
            final Optional<String> acceptedTimestamp =
                    responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
            if (acceptedTimestamp.isEmpty()) {
                throw new IllegalArgumentException("No timestamp header");
            }
            return HttpResponse.BodySubscribers.replacing(Response.ACCEPTED);
        } else {
            throw new RejectedExecutionException("Can't get response");
        }
    }
}
