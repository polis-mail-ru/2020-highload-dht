package ru.mail.polis.service.mariarheon;

import one.nio.http.Response;

import java.util.Date;

/**
 * This class is used for composing response to client
 * by responses retrieved from replicas.
 */
public class ReplicasResponseComposer {
    private int ackCount;
    private int goodAnswers;
    private int status;
    private Record record;
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";

    /**
     * Create composer for generating response for client from replicas answers.
     *
     * @param ackCount - count of required acknowledgements.
     */
    public ReplicasResponseComposer(final int ackCount) {
        this.ackCount = ackCount;
    }

    /**
     * Add response from replica.
     *
     * @param response - response from replica.
     */
    public void addResponse(final Response response) {
        final var status = response.getStatus();
        if (status < 200 || status > 202) {
            return;
        }
        goodAnswers++;
        this.status = status;
        if (status == 200) {
            final var responseRecord = Record.newFromRawValue(response.getBody());
            if (this.record == null ||
                    (!responseRecord.wasNotFound() &&
                    responseRecord.getTimestamp().after(this.record.getTimestamp()))) {
                this.record = responseRecord;
            }
        }
    }

    /**
     * Get response for client, combined from responses from replicas.
     *
     * @return - response for client.
     */
    public Response getComposedResponse() {
        if (goodAnswers < ackCount) {
            return new Response(NOT_ENOUGH_REPLICAS, Response.EMPTY);
        }
        if (status == 201) {
            return new Response(Response.CREATED, Response.EMPTY);
        }
        if (status == 202) {
            return new Response(Response.ACCEPTED, Response.EMPTY);
        }
        if (record.wasNotFound() || record.isRemoved()) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return Response.ok(record.getValue());
    }
}
