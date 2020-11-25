package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.s3ponia.Value;
import ru.mail.polis.util.Utility;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;

public class GetBodyHandler implements HttpResponse.BodyHandler<Value> {
    @Override
    public HttpResponse.BodySubscriber<Value> apply(@NotNull final HttpResponse.ResponseInfo responseInfo) {
        if (responseInfo.statusCode() != 200 /* OK */
                && responseInfo.statusCode() != 404 /* NOT FOUND */) {
            throw new IllegalArgumentException("Error in get request");
        }

        final var header = responseInfo.headers().firstValue(Utility.DEADFLAG_TIMESTAMP_HEADER);
        if (header.isEmpty()) {
            return HttpResponse.BodySubscribers.replacing(Value.ABSENT);
        }

        final var deadFlagTimestamp = Long.parseLong(header.get());

        if (responseInfo.statusCode() == 404 /* NOT FOUND */) {
            return HttpResponse.BodySubscribers.replacing(Value.dead(-1, deadFlagTimestamp));
        }

        return HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofByteArray(),
                (b) -> {
                    final var bBuffer = ByteBuffer.wrap(b);
                    return Value.of(bBuffer, deadFlagTimestamp, -1);
                }
        );
    }
}
