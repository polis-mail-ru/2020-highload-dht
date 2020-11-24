package ru.mail.polis.service.mariarheon;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used for composing response to client
 * by responses retrieved from replicas.
 */
public class ReplicasResponseComposer {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    private final Replicas replicas;
    private int ackReceived;
    private int totalReceived;
    private int status;
    private Record record;
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private final Map<String, Record> goodAnswers;
    private Response preparedResponse;

    /**
     * Create composer for generating response for client from replicas answers.
     *
     * @param replicas - count of required acknowledgements and total nodes.
     */
    public ReplicasResponseComposer(final Replicas replicas) {
        this.replicas = replicas;
        this.goodAnswers = new HashMap<>();
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
            this.goodAnswers.put(fromNode, responseRecord);
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

    /**
     * Retrieve the data necessary for repairing nodes.
     *
     * @param key - key of the record which should be repaired on nodes.
     * @return - data necessary for repairing nodes.
     */
    public ReadRepairInfo getReadRepairInfo(String key) {
        prepareResponse();
        if (preparedResponse.getStatus() != 200) {
            return null;
        }
        final var recordWithKey = Record.newFromRawValue(key, record.getRawValue());
        final var res = new ReadRepairInfo(recordWithKey);
        logger.info("\n#answer ="
                + Util.loggingValue(record.getValue())
                + " timestamp=(" + record.getTimestamp().getTime() + ")");
        for (var recordByNode : goodAnswers.entrySet()) {
            final var node = recordByNode.getKey();
            final var answerFromNode = recordByNode.getValue();
            logger.info("\n#answer from " + node + " ="
                    + Util.loggingValue(answerFromNode.getValue())
                    + " timestamp=(" + answerFromNode.getTimestamp().getTime() + ")");
            if (answerFromNode.getTimestamp().before(record.getTimestamp())) {
                res.addNode(node);
            }
        }
        return res;
    }
}
