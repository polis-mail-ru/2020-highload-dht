package ru.mail.polis.service.codearound;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Includes methods for processing node replication factors from validating parsed input
 * through type-specific instance return.
 */
class ReplicationFactor {

    private static final String REPLIFACTOR_ERROR_LOG = "One or both of replication factors can't be "
           + "handled appropriately, validation failed";
    private final int ack;
    private final int from;

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
     * @return reference on ReplicationFactor class instance
     */
    private static ReplicationFactor createRepliFactor(final String values) {
        final List<String> delimitValues = Arrays.asList(values.replace("=", "").split("/"));
        if (delimitValues.size() != 2) {
            throw new IllegalArgumentException("Either one of factors is missing or both don't exist");
        }
        if (Integer.parseInt(delimitValues.get(0)) < 1 || Integer.parseInt(delimitValues.get(1)) < 1) {
            throw new IllegalArgumentException("Each factor integer should be positive");
        }
        if (Integer.parseInt(delimitValues.get(0)) > Integer.parseInt(delimitValues.get(1))) {
            throw new IllegalArgumentException("'ack' factor input should be less or equal one of 'from'");
        }
        return new ReplicationFactor(
                Integer.parseInt(delimitValues.get(0)),
                Integer.parseInt(delimitValues.get(1)));
    }

    /**
     * sets reference on class instance, passes that into service impl context.
     *
     * @param replicas - String argument to be processed for primary evaluation of both 'ack' and 'from' factors
     * @param repliFactor  - ReplicationFactor instance
     * @return reference on ReplicationFactor class instance
     */
    static ReplicationFactor defaultRepliFactor(
            final String replicas,
            final ReplicationFactor repliFactor,
            @NotNull final HttpSession session) throws IOException {
        ReplicationFactor retRepliFactor = null;
        try {
            if (replicas == null) {
                retRepliFactor = repliFactor;
            } else {
                retRepliFactor = ReplicationFactor.createRepliFactor(replicas);
            }
        } catch (IllegalArgumentException exc) {
           session.sendError(Response.BAD_REQUEST, REPLIFACTOR_ERROR_LOG);
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
