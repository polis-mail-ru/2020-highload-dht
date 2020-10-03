package ru.mail.polis.service.art241111.handlers;

import one.nio.http.Response;

public final class StatusHandler {
    /**
     * Response to a status request.
     * @return Server status.
     */
    public Response handlerStatusRequest() {
        return new Response(Response.OK, Response.EMPTY);
    }
}
