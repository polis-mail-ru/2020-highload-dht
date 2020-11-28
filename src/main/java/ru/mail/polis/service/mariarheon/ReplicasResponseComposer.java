package ru.mail.polis.service.mariarheon;

import one.nio.http.Response;

/**
 * This class is used for composing response to client
 * by responses retrieved from replicas.
 */
public class ReplicasResponseComposer {
    private final Replicas replicas;
    private int ackReceived;
    private int totalReceived;
    private int status;
    private Record record;
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private Response preparedResponse;

    /**
     * Create composer for generating response for client from replicas answers.
     *
     * @param replicas - count of required acknowledgements and total nodes.
     */
    public ReplicasResponseComposer(final Replicas replicas) {
        this.replicas = replicas;
    }

    /**
     * Add response from replica.
     *
     * @param response - response from replica.
     */
    public void addResponse(final String fromNode, final Response response) {
        preparedResponse = null;
        totalReceived++;
        final var responseStatus = response.getStatus();
        if (responseStatus < 200 || responseStatus > 202) {
            return;
        }
        ackReceived++;
        this.status = responseStatus;
        if (responseStatus == 200) {
            final var responseRecord = Record.newFromRawValue(response.getBody());
            if (this.record == null
                    || (!responseRecord.wasNotFound()
                    && responseRecord.getTimestamp().after(this.record.getTimestamp()))) {
                this.record = responseRecord;
            }
        }
    }

    /**
     * Returns true if ack good answers was reached or we get responses from all the nodes.
     *
     * @return - true if ack good answers was reached or we get responses from all the nodes.
     */
    public boolean answerIsReady() {
        return ackReceived >= replicas.getAckCount() || totalReceived >= replicas.getTotalNodes();
    }

    private Response getPreparedResponse() {
        if (ackReceived < replicas.getAckCount()) {
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

    private void prepareResponse() {
        if (preparedResponse != null) {
            return;
        }
        preparedResponse = getPreparedResponse();
    }

    /**
     * Get response for client, combined from responses from replicas.
     *
     * @return - response for client.
     */
    public Response getComposedResponse() {
        prepareResponse();
        return preparedResponse;
    }
}
