package ru.mail.polis.service.zvladn7.bodyhandlers;

import one.nio.http.Response;
import java.net.http.HttpResponse;

public class ChangeBodyHandler implements HttpResponse.BodyHandler<String> {

    public static final ChangeBodyHandler INSTANCE = new ChangeBodyHandler();
    private static final int DELETE_SUCCESS_STATUS_CODE = 202;
    private static final int UPSERT_SUCCESS_STATUS_CODE = 201;

    private ChangeBodyHandler() {
    }

    @Override
    public HttpResponse.BodySubscriber<String> apply(HttpResponse.ResponseInfo responseInfo) {
        switch (responseInfo.statusCode()) {
            case UPSERT_SUCCESS_STATUS_CODE:
                return HttpResponse.BodySubscribers.replacing(Response.CREATED);
            case DELETE_SUCCESS_STATUS_CODE:
                return HttpResponse.BodySubscribers.replacing(Response.ACCEPTED);
            default:
                throw new IllegalArgumentException("Unknown method for ChangeBodyHandler");
        }
    }

}
