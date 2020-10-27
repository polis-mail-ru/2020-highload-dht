package ru.mail.polis.service.ivlev.util;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Value;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.ivlev.ThreadController;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public final class SendResponsesUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadController.class);
    private static final String RESPONSE_ERROR_LOG_MESSAGE = "Fail send response: ";
    private static final String IS_REMOVED_FLAG = "isRemoved";

    private SendResponsesUtils() {
    }

    /**
     * Ответ на прокси запрос.
     *
     * @param id      - key
     * @param session - session
     * @param request - request
     */
    public static void sendProxyResponse(
            final String id,
            final HttpSession session,
            final Request request,
            final DAO dao) {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    session.sendResponse(get(id, dao));
                    break;
                case Request.METHOD_PUT:
                    session.sendResponse(put(id, request, dao));
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(delete(id, dao));
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, ex);
        }
    }

    /**
     * End-point get.
     *
     * @param id - id
     */
    public static Response get(@NotNull final String id, @NotNull final DAO dao) {
        try {
            final Value value = dao.getCell(toByteBuffer(id)).getValue();
            final Response response;
            if (value.isRemoved()) {
                response = new Response(Response.OK, Response.EMPTY);
            } else {
                response = new Response(Response.OK, toByteArray(value.getData()));
            }
            response.addHeader(IS_REMOVED_FLAG + value.isRemoved());
            return response;
        } catch (NoSuchElementException ex) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        } catch (IOException ex) {
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * End-point put.
     *
     * @param id      - id
     * @param request - http request
     */
    public static Response put(final @NotNull String id, final Request request, @NotNull final DAO dao) {
        try {
            dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, id, request.getBody(), e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * End-point delete.
     *
     * @param id  - id
     * @param dao - dao
     * @return - Response
     */
    public static Response delete(@NotNull final String id, @NotNull final DAO dao) {
        try {
            dao.remove(toByteBuffer(id));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            LOGGER.error(RESPONSE_ERROR_LOG_MESSAGE, id, e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private static ByteBuffer toByteBuffer(@NotNull final String id) {
        return ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] toByteArray(@NotNull final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }
        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }
}
