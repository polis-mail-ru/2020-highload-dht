package ru.mail.polis.service.suhova;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.suhova.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public final class DAOServiceMethods {
    private static final Logger logger = LoggerFactory.getLogger(MoribundService.class);

    /**
     * Get a value from DAO and send response with it.
     *
     * @param id  - key
     * @param dao - dao
     * @return response
     */
    public static Response get(@NotNull final String id, @NotNull DAO dao) {
        try {
            final Value value = dao.getCell(toByteBuffer(id)).getValue();
            final Response response;
            if (value.isTombstone()) {
                response = new Response(Response.OK, Response.EMPTY);
            } else {
                response = new Response(Response.OK, toByteArray(value.getData()));
            }
            response.addHeader("isTombstone" + value.isTombstone());
            return response;
        } catch (NoSuchElementException e) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    /**
     * Put a value into DAO and send response about it.
     *
     * @param id  - key
     * @param dao - dao
     * @return response
     */
    public static Response put(final @NotNull String id, final Request request, @NotNull DAO dao) {
        try {
            dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
            return new Response(Response.CREATED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("FAIL PUT! id: {}, request: {}, error: {}", id, request.getBody(), e.getMessage());
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    /**
     * Delete a value from DAO and send response about it.
     *
     * @param id  - key
     * @param dao - dao
     * @return response
     */
    public static Response delete(@NotNull final String id, @NotNull DAO dao) {
        try {
            dao.remove(toByteBuffer(id));
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } catch (IOException e) {
            logger.error("FAIL DELETE! id: {}, error: {}", id, e.getMessage());
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
