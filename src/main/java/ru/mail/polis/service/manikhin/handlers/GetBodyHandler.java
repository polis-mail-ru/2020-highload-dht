package ru.mail.polis.service.manikhin.handlers;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.manikhin.TimestampRecord;

import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;

public final class GetBodyHandler implements HttpResponse.BodyHandler<TimestampRecord> {

    public static final HttpResponse.BodyHandler<TimestampRecord> INSTANCE = new GetBodyHandler();
    private GetBodyHandler() { }

    @Override
    public HttpResponse.BodySubscriber<TimestampRecord> apply(@NotNull final HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case 200:
            case 404:
                return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofByteArray(),
                            TimestampRecord::fromBytes);
            default:
                throw new RejectedExecutionException("Can't get response");
        }
    }
}