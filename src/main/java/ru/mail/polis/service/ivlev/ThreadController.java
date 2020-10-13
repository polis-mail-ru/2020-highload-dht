package ru.mail.polis.service.ivlev;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadController extends HttpServer implements Service {

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executor;
    private static final Logger logger = LoggerFactory.getLogger(ThreadController.class);

    /**
     * Implementation {@link Service}.
     *
     * @param config       - config
     * @param dao          - dao
     * @param workersCount - count of executor workers
     * @param queueSize    - ArrayBlockingQueue max size
     */
    public ThreadController(final HttpServerConfig config,
                            @NotNull final DAO dao,
                            final int workersCount,
                            final int queueSize) throws IOException {
        super(config, dao);
        assert workersCount > 0;
        assert queueSize > 0;
        this.dao = dao;
        executor = new ThreadPoolExecutor(
                workersCount, queueSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Exception {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Path("/v0/status")
    public void status(final HttpSession session) {
        executor.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                logger.error("Fail send response: ", e);
            }
        });
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id, final HttpSession session) {
        executor.execute(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                session.sendResponse(new Response(Response.OK, toByteArray(dao.get(toByteBuffer(id)))));
            } catch (NoSuchElementException e) {
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Fail send response: ", ioException);
                }
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Fail send response: ", ioException);
                }
            }
        });
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(value = "id", required = true) final String id,
                    final HttpSession session,
                    final Request request) {
        executor.execute(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                dao.upsert(toByteBuffer(id), ByteBuffer.wrap(request.getBody()));
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Fail send response: ", ioException);
                }
            }
        });
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(value = "id", required = true) final String id, final HttpSession session) {
        executor.execute(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                dao.remove(toByteBuffer(id));
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Fail send response: ", ioException);
                }
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        executor.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Fail send response: ", e);
            }
        });
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
