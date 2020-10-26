package ru.mail.polis.service.mariarheon;

public class Replicas {
    private int ackCount;
    private int totalNodes;

    private static final String SLASH_SHOULD_BE_PRESENTED_ERROR_MSG = "/-symbol should be in the value";
    private static final String ACK_TO_HIGH_ERROR_MSG = "Ack-value should be less than or equal to from-value";
    private static final String ACK_TO_LOW_ERROR_MSG = "Ack-value should be more than 0";
    private static final String NON_INTEGER_IN_VALUE_ERROR_MSG = "Two numbers should be here separated by /-symbol";

    /**
     * Storing the value of 'replicas'-param of queries.
     *
     * @param ackFromString - the value of 'replicas'-param.
     * @throws ReplicasParamParseException - throws when the format of the value is incorrect.
     */
    public Replicas(final String ackFromString, final int nodeCount) throws ReplicasParamParseException {
        if (ackFromString == null) {
            this.ackCount = nodeCount / 2 + 1;
            this.totalNodes = nodeCount;
            return;
        }
        final int slashWhere = ackFromString.indexOf('/');
        if (slashWhere == -1) {
            throw new ReplicasParamParseException(SLASH_SHOULD_BE_PRESENTED_ERROR_MSG);
        }
        final String ackStr = ackFromString.substring(0, slashWhere);
        final String fromStr = ackFromString.substring(slashWhere + 1);
        try {
            this.ackCount = Integer.parseInt(ackStr);
            this.totalNodes = Integer.parseInt(fromStr);
        } catch (NumberFormatException ex) {
            throw new ReplicasParamParseException(NON_INTEGER_IN_VALUE_ERROR_MSG, ex);
        }
        if (this.totalNodes > nodeCount) {
            this.totalNodes = nodeCount;
        }
        if (this.ackCount > this.totalNodes) {
            throw new ReplicasParamParseException(ACK_TO_HIGH_ERROR_MSG);
        }
        if (this.ackCount <= 0) {
            throw new ReplicasParamParseException(ACK_TO_LOW_ERROR_MSG);
        }
    }

    /**
     * Returns number of answers we should get in order to think that this is enough.
     *
     * @return - number of answers we should get in order to think that this is enough.
     */
    public int getAckCount() {
        return ackCount;
    }

    /**
     * Returns number of nodes which should be used for this operation.
     *
     * @return - number of nodes which should be used for this operation.
     */
    public int getTotalNodes() {
        return totalNodes;
    }
}
