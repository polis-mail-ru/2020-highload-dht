package ru.mail.polis.service.art241111.handlers;

import one.nio.http.Response;

public class StatusHandlers {
    /**
     * Response to a status request.
     * @return Server status.
     */
    public Response setStatusHandler() {
        return new Response(Response.OK, Response.EMPTY);
    }
}
