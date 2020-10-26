package ru.mail.polis.service.codearound;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Includes methods for processing node replication factors from validating parsed input
 * through type-specific instance return.
 */
class ReplicationFactor {

    private final int ack;
    private final int from;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationFactor.class);
    private static final String REPLIFACTOR_ERROR_LOG = "One or both of replication factors can't be "
            + "handled appropriately, validation failed";

    /**
     * class instance const.
     *
     * @param ack  - quorum factor
     * @param from - general replica quantity factor (necessary to be greater or equal 'ack')
     */
    ReplicationFactor(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    /**
     * invokes class const, retrieves instance.
     *
     * @param values  - String argument to be processed for primary evaluation of both 'ack' and 'from' factors
     * @param session - ongoing HTTP session
     * @return reference on ReplicationFactor class instance
     */
    private static ReplicationFactor createRepliFactor(
            final String values,
            @NotNull final HttpSession session) throws IOException {

        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));

        if (delimitValues.size() != 2) {
            LOGGER.error("Either one of factors is missing or both don't exist");
            session.sendError(Response.BAD_REQUEST, REPLIFACTOR_ERROR_LOG);
        }
        if (Integer.parseInt(delimitValues.get(0)) < 1 || Integer.parseInt(delimitValues.get(1)) < 1) {
            LOGGER.error("Each factor integer should be positive");
            session.sendError(Response.BAD_REQUEST, REPLIFACTOR_ERROR_LOG);
        }
        if (Integer.parseInt(delimitValues.get(0)) > Integer.parseInt(delimitValues.get(1))) {
            LOGGER.error("'ack' factor input should be less or equal one of 'from'");
            session.sendError(Response.BAD_REQUEST, REPLIFACTOR_ERROR_LOG);
        }
        return new ReplicationFactor(
                Integer.parseInt(delimitValues.get(0)),
                Integer.parseInt(delimitValues.get(1)));
    }

    /**
     * sets reference on class instance, passes that into service impl context.
     *
     * @param nodeReplicas - String argument to be processed for primary evaluation of both 'ack' and 'from' factors
     * @param session      - ongoing HTTP session
     * @param repliFactor  - ReplicationFactor instance
     * @return reference on ReplicationFactor class instance
     */
    static ReplicationFactor getRepliFactor(
            final String nodeReplicas,
            final HttpSession session,
            final ReplicationFactor repliFactor) throws IOException {

        ReplicationFactor retRepliFactor = null;
        if (nodeReplicas == null) {
            retRepliFactor = repliFactor;
        } else {
            retRepliFactor = ReplicationFactor.createRepliFactor(nodeReplicas, session);
        }
        return retRepliFactor;
    }

    /**
     * retrieves 'ack' factor evaluation result.
     */
    int getAckValue() {
        return ack;
    }

    /**
     * retrieves 'from' factor evaluation result.
     */
    int getFromValue() {
        return from;
    }
}
