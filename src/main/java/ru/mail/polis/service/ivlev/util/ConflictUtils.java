package ru.mail.polis.service.ivlev.util;

import one.nio.http.Response;

import java.util.List;

public final class ConflictUtils {
    private static final String NOT_ENOUGH_REPLICA = "504 Not Enough Replicas";
    private static final String IS_REMOVED_FLAG = "isRemoved";

    private static final int NOT_FOUND_STATUS = 404;
    private static final int OK_STATUS = 200;
    private static final int CREATED_STATUS = 201;
    private static final int ACCEPTED_STATUS = 202;

    private ConflictUtils() {
    }

    /**
     * Решение конфилкта get запроса.
     *
     * @param responses - list of all responses
     * @param confirm   - confirm
     * @return resulting response
     */
    public static Response get(final List<Response> responses, final int confirm) {
        int count = 0;
        int countNotFound = 0;
        boolean isDeleted = false;
        Response okResponse = new Response(Response.NOT_FOUND, Response.EMPTY);
        for (final Response response : responses) {
            final int status = response.getStatus();
            if (status == OK_STATUS) {
                count++;
                if (Boolean.parseBoolean(response.getHeader(IS_REMOVED_FLAG))) {
                    isDeleted = true;
                    continue;
                }
                okResponse = response;
            } else if (status == NOT_FOUND_STATUS) {
                countNotFound++;
                count++;
            }
        }
        if (count >= confirm) {
            if (isDeleted || count == countNotFound) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } else {
                return Response.ok(okResponse.getBody());
            }
        } else {
            return new Response(NOT_ENOUGH_REPLICA, Response.EMPTY);
        }
    }

    /**
     * Решение конфилкта put запроса.
     *
     * @param responses - list of all responses
     * @param confirm   - confirm
     * @return resulting response
     */
    public static Response put(final List<Response> responses, final int confirm) {
        return simpleResponse(responses, confirm, CREATED_STATUS, Response.CREATED);
    }

    /**
     * Решение конфилкта delete запроса.
     *
     * @param responses - list of all responses
     * @param confirm   - confirm
     * @return resulting response
     */
    public static Response delete(final List<Response> responses, final int confirm) {
        return simpleResponse(responses, confirm, ACCEPTED_STATUS, Response.ACCEPTED);
    }

    private static Response simpleResponse(
            final List<Response> responses,
            final int confirm,
            final int status,
            final String result) {
        int count = 0;
        for (final Response response : responses) {
            if (response.getStatus() == status) {
                count++;
            }
        }
        if (count >= confirm) {
            return new Response(result, Response.EMPTY);
        } else {
            return new Response(NOT_ENOUGH_REPLICA, Response.EMPTY);
        }
    }
}
