package ru.mail.polis.service.alexander.marashov;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ValidatedParameters {
    final int ack;
    final int from;
    final ByteBuffer key;

    /**
     * Object to store validated parameters.
     * @param ack - ack parameter.
     * @param from - from parameter.
     * @param key - key parameter.
     */
    public ValidatedParameters(final int ack, final int from, final ByteBuffer key) {
        this.ack = ack;
        this.from = from;
        this.key = key;
    }

    /**
     * Validates parameters and throws exceptions if parameters are invalid.
     * @param id - id parameter.
     * @param replicas - replicas parameter, has ack/from format.
     * @return validated parameters.
     * @throws IllegalArgumentException if any parameter has invalid value.
     */
    public static ValidatedParameters validateParameters(
            final String id,
            final String replicas,
            final int defaultAck,
            final int defaultFrom,
            final int nodesCount
    ) throws IllegalArgumentException {
        final int[] replicasParameters = unpackReplicasParameter(replicas);
        final int ack;
        final int from;
        if (replicasParameters.length == 0) {
            ack = defaultAck;
            from = defaultFrom;
        } else {
            ack = replicasParameters[0];
            from = replicasParameters[1];
        }

        if (areParametersWrong(id, ack, from, nodesCount)) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        final byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer key = ByteBuffer.wrap(bytes);

        return new ValidatedParameters(ack, from, key);
    }

    private static boolean areParametersWrong(
            final String id,
            final int ack,
            final int from,
            final int nodesCount
    ) {
        return id.isEmpty() || ack <= 0 || ack > from || from > nodesCount;
    }

    private static int[] unpackReplicasParameter(final String replicas) throws NumberFormatException {
        if (replicas == null) {
            return new int[0];
        }
        final int firstSlashIndex = replicas.indexOf('/');
        final int lastSlashIndex = replicas.lastIndexOf('/');
        if (firstSlashIndex != lastSlashIndex || firstSlashIndex == -1) {
            return new int[0];
        }

        final int ack = Integer.parseInt(replicas.substring(0, firstSlashIndex));
        final int from = Integer.parseInt(replicas.substring(firstSlashIndex + 1));
        return new int[]{ack, from};
    }
}
