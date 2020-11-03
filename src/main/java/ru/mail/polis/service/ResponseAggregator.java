package ru.mail.polis.service;

import one.nio.http.Response;

/**
 * This class aggregates received responses from replicas.
 */
class ResponseAggregator {
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    
    private Response required;
    private int count;
    private boolean isRemoved;
    private int oks;
    private int notOks;

    void add(final Response response, final int count) {
        this.count = count;
        if (response.getStatus() == 310) {
            isRemoved = true;
        }
        if (response.getStatus() == 404) {
            notOks++;
        }
        if (response.getStatus() >= 200 && response.getStatus() <= 202) {
            if (required == null) {
                required = response;
            }
            oks++;
        }
    }

    Response getResult() {
        Response response = required;
        final int countRetrieved = oks + notOks;
        if (isRemoved) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        } else if (countRetrieved < this.count) {
            response = new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        } else if (oks == 0 && notOks > 0) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return response;
    }
}
