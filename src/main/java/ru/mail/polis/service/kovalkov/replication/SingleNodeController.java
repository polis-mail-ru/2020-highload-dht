package ru.mail.polis.service.kovalkov.replication;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static ru.mail.polis.service.kovalkov.ReplicationServiceImpl.exceptionIOHandler;

public class SingleNodeController {
    private static final Logger log = LoggerFactory.getLogger(SingleNodeController.class);
    private final DAO dao;
    private final ExecutorService service;

    public SingleNodeController(@NotNull final DAO dao, @NotNull final ExecutorService service) {
        this.dao = dao;
        this.service = service;
    }

    /**
     * Async get with for one node.
     *
     * @param id requested id.
     * @param session http user session.
     */
    public void asyncGet(@NotNull final ByteBuffer id, @NotNull final HttpSession session) {
        try {
            service.execute(() -> {
                getInternal(id, session);
            });
        } catch (RejectedExecutionException e) {
            log.error("Rejected single get", e);
            try {
                session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE, Response.EMPTY));
            } catch (IOException e1) {
                log.error("IO exception when send 503 response: ", e1);
            }
        }
    }

    private void getInternal(@NotNull final ByteBuffer key,
                             @NotNull final HttpSession session) {
        try {
            final ByteBuffer value = dao.get(key);
            final byte[] bytes = BufferConverter.unfoldToBytes(value);
            session.sendResponse(Response.ok(bytes));
        } catch (IOException e) {
            exceptionIOHandler(session, "Method get. IO exception. ", e);
        } catch (NoSuchElementException e) {
            log.info("Method get. Can't find value by this key ", e);
            try {
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException ioException) {
                log.error("Method get. Id is empty. Can't send response:", e);
            }
        }
    }

    /**
     * Async put with for one node.
     *
     * @param id requested id.
     * @param request  value who will be put to db.
     * @param session http user session.
     */
    public void asyncPut(@NotNull final ByteBuffer id,
                         @NotNull final Request request, @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                putInternal(id, request, session);
            } catch (IOException e) {
                exceptionIOHandler(session, "IO exception. Put ", e);
            }
        });
    }

    private void putInternal(@NotNull final ByteBuffer key,
                             @NotNull final Request request,
                             @NotNull final HttpSession session) throws IOException {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        dao.upsert(key, value);
        session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
    }

    /**
     * Async delete with for one node.
     *
     * @param id requested id.
     * @param session http user session.
     */
    public void asyncDelete(@NotNull final ByteBuffer id, @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                deleteInternal(id, session);
            } catch (IOException e) {
                exceptionIOHandler(session, "Method delete. IO exception. ", e);
            }
        });
    }

    private void deleteInternal(@NotNull final ByteBuffer key,
                                @NotNull final HttpSession session) throws IOException {
        dao.remove(key);
        session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
    }
}
