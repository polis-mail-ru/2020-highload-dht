package ru.mail.polis.service.mrsandman5.handlers;

import org.jetbrains.annotations.NotNull;;
import ru.mail.polis.utils.ResponseUtils;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

public class PutBodyHandler implements HttpResponse.BodyHandler<Void> {

    public static final HttpResponse.BodyHandler<Void> INSTANCE = new PutBodyHandler();

    private PutBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<Void> apply(
            @NotNull final HttpResponse.ResponseInfo responseInfo) {
        final int status = responseInfo.statusCode();
        if (status == 201) {
            final Optional<String> createdTimestamp =
                    responseInfo.headers().firstValue(ResponseUtils.TIMESTAMP);
            if (createdTimestamp.isEmpty()) {
                throw new IllegalArgumentException("No timestamp header");
            }
            return HttpResponse.BodySubscribers.discarding();
        } else {
            throw new RejectedExecutionException("Can't get response");
        }
    }
}
