package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.util.Collection;

public final class ValuesAnalyzer {

    private ValuesAnalyzer() {

    }

    /**
     * Analyze the collection of values to get correct Response instance.
     * @param collection - collection of calculated values.
     * @return correct Response instance.
     */
    public static Response analyze(final Collection<Value> collection) {
        Value correctValue = null;
        for (final Value value : collection) {
            if (correctValue == null) {
                correctValue = value;
            } else {
                if (value.compareTo(correctValue) < 0) {
                    correctValue = value;
                }
            }
        }

        if (correctValue == null || correctValue.isTombstone()) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return new Response(Response.OK, correctValue.getData().array());
    }
}
