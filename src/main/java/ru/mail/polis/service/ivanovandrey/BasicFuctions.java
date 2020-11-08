package ru.mail.polis.service.ivanovandrey;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BasicFuctions {
    private static final String ERROR_MESSAGE = "Can't send response. Session {}";
    private static final String RESPONSE_ERROR = "Can not send response.";

    private final DAO dao;
    private final ExecutorService execPool;
    private final Logger log = LoggerFactory.getLogger(BasicFuctions.class);

    /**
     * Constructor.
     * @param dao       - dao.
     * @param execPool  - ExecutorService.
     */
    public BasicFuctions(@NotNull final DAO dao,
                         @NotNull final ExecutorService execPool) {
        this.dao = dao;
        this.execPool = execPool;
    }

    /**
     * Get data by key.
     * @param id - key.
     */
    public CompletableFuture<Response> get(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final ByteBuffer val = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return Response.ok(Util.fromByteBufferToByteArray(val));
            } catch (NoSuchElementException e) {
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            } catch (IOException e) {
                log.error("Error in get request", e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Put data by key.
     * @param id      - key.
     * @param request - request.
     */
    public CompletableFuture<Response> put(final String id, final byte[] request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.upsertWithTimestamp(
                        ByteBuffer.wrap(id.getBytes(UTF_8)),
                        ByteBuffer.wrap(request));
                return new Response(Response.CREATED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(ERROR_MESSAGE, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Delete data by key.
     * @param id - key.
     */
    public CompletableFuture<Response> delete(final String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.removeWithTimestamp(ByteBuffer.wrap(id.getBytes(UTF_8)));
                return new Response(Response.ACCEPTED, Response.EMPTY);
            } catch (IOException ex) {
                log.error(ERROR_MESSAGE, ex);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }, execPool);
    }

    /**
     * Send response when future completes.
     * @param session - session.
     * @param response - response.
     */
    public void trySendResponse(final HttpSession session,
                                final CompletableFuture<Response> response) {
        if (response.whenComplete((r, t) -> {
            if (t == null) {
                try {
                    session.sendResponse(r);
                } catch (IOException ex) {
                    log.error(RESPONSE_ERROR, ex);
                }
            } else {
                try {
                    session.sendError(ERROR_MESSAGE, t.getMessage());
                } catch (IOException ex) {
                    log.error("Can not send error.", ex);
                }
            }
        }).isCancelled()) {
            log.error(RESPONSE_ERROR);
        }
    }
}
