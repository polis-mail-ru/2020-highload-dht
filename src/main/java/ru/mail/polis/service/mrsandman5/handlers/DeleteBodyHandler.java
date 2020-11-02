package ru.mail.polis.service.mrsandman5.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

public class DeleteBodyHandler implements HttpResponse.BodyHandler<Void> {

    public static final HttpResponse.BodyHandler<Void> INSTANCE = new DeleteBodyHandler();

    private DeleteBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Void> apply(
            @NotNull final HttpResponse.ResponseInfo responseInfo) {
        final int status = responseInfo.statusCode();
        if (status == 202) {
            final Optional<String> acceptedTimestamp =
                    responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
            if (acceptedTimestamp.isEmpty()) {
                throw new IllegalArgumentException("No timestamp header");
            }
            return HttpResponse.BodySubscribers.discarding();
        } else {
            throw new RejectedExecutionException("Can't get response");
        }
    }
}
