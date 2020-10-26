package ru.mail.polis.service.kate.moreva;

import one.nio.http.Request;
import one.nio.http.Response;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class MyRequestHelper {
    private static final String TIMESTAMP = "Timestamp: ";
    private static final String PROXY_HEADER = "X-Proxy-For:";

    public Response mergeResponses(final List<Response> result) {
        final Map<Response, Integer> responses = new ConcurrentSkipListMap<>(Comparator.comparing(this::getStatus));
        result.forEach(resp -> {
            final Integer val = responses.get(resp);
            responses.put(resp, val == null ? 0 : val + 1);
        });
        Response finalResult = null;
        int maxCount = -1;
        long time = Long.MIN_VALUE;
        for (final Map.Entry<Response, Integer> entry : responses.entrySet()) {
            if (entry.getValue() >= maxCount && getTimestamp(entry.getKey()) > time) {
                time = getTimestamp(entry.getKey());
                maxCount = entry.getValue();
                finalResult = entry.getKey();
            }
        }
        return finalResult;
    }

    public String getStatus(final Response response) {
        return response.getHeaders()[0];
    }

    public boolean isProxied(final Request request) {
        return request.getHeader(PROXY_HEADER) != null;
    }

    public static long getTimestamp(final Response response) {
        final String timestamp = response.getHeader(TIMESTAMP);
        return timestamp == null ? -1 : Long.parseLong(timestamp);
    }
}
