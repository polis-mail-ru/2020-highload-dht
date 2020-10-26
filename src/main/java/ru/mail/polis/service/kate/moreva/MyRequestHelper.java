package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class MyRequestHelper {
    private static final String SERVER_ERROR = "Server error can't send response";
    private static final String TIMESTAMP = "Timestamp: ";
    private static final String PROXY_HEADER = "X-Proxy-For:";
    private static final String NOT_ENOUGH_REPLICAS = "504 Not Enough Replicas";
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpServer.class);

    /**
     * Merges responses for GET request.
     * */
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

    public void correctReplication(final int ack,
                                    final Replicas replicas,
                                    final HttpSession session,
                                    final String response) {
        try {
            if (ack < replicas.getAck()) {
                handleResponse(session, NOT_ENOUGH_REPLICAS);
            } else {
                session.sendResponse(new Response(response, Response.EMPTY));
            }
        } catch (IOException e) {
            handleResponse(session, Response.INTERNAL_ERROR);
        }
    }

    public void handleResponse(final HttpSession session, final String response) {
        try {
            session.sendResponse(new Response(response, Response.EMPTY));
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }

    public void sendLoggedResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }
}
