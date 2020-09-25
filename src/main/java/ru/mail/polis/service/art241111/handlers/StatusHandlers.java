package ru.mail.polis.service.art241111.handlers;

import one.nio.http.Response;

public class StatusHandlers {
    public Response setStatusHandler(){
        return new Response(Response.OK, Response.EMPTY);
    }
}
