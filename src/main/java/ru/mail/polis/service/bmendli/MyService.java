package ru.mail.polis.service.bmendli;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.bmendli.NoSuchElementExceptionLightWeight;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MyService extends HttpServer implements Service {

    private final Logger logger = LoggerFactory.getLogger(MyService.class);

    @NotNull
    private final DAO dao;
    @NotNull
    private final ExecutorService executorService;

    /**
     * My implementation of {@link Service}.
     */
    public MyService(final int port,
                     @NotNull final DAO dao,
                     final int threadCount,
                     final int queueCapacity) throws IOException {
        super(createConfigFromPort(port, threadCount));
        this.dao = dao;
        this.executorService = new ThreadPoolExecutor(
                threadCount,
                queueCapacity,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder()
                        .setNameFormat("2020-highload-dht-thread-%d")
                        .setUncaughtExceptionHandler((thread, e) -> logger.error("error in {} thread", thread, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Get request. Return a data which associated with
     * passed id in path '/v0/entity' from dao.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@NotNull @Param(required = true, value = "id") final String id,
                    @NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                if (id.isBlank()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final byte[] bytes = id.getBytes(Charsets.UTF_8);
                final ByteBuffer wrappedBytes = ByteBuffer.wrap(bytes);
                final ByteBuffer byteBuffer = dao.get(wrappedBytes);
                session.sendResponse(Response.ok(getBytesFromByteBuffer(byteBuffer)));
            } catch (NoSuchElementExceptionLightWeight e) {
                logger.error("Does not exist record by id = {}", id, e);
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Server error, cant send response for session {}", session, e);
                }
            } catch (IOException ioe) {
                logger.error("Error when trying get record", ioe);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Server error, cant send response for session {}", session, e);
                }
            }
        });
    }

    /**
     * Put request. Put data in dao which associated with
     * passed id in path '/v0/entity'.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@NotNull @Param(required = true, value = "id") final String id,
                    @NotNull final Request request,
                    @NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                if (id.isBlank()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException ioe) {
                logger.error("Error when trying put record", ioe);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Server error, cant send response for session {}", session, e);
                }
            }
        });
    }

    /**
     * Delete request. Delete data from dao which associated with
     * passed id in path '/v0/entity'.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@NotNull @Param(required = true, value = "id") final String id,
                           @NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                if (id.isBlank()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException ioe) {
                logger.error("Error when trying delete record", ioe);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException e) {
                    logger.error("Server error, cant send response for session {}", session, e);
                }
            }
        });
    }

    /**
     * Return status for path '/v0/status'.
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        executorService.execute(() -> {
            try {
                session.sendResponse(Response.ok(Response.EMPTY));
            } catch (IOException e) {
                logger.error("Server error, cant send response for session {}", session, e);
            }
        });
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        executorService.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Server error, cant send response for session {}", session, e);
            }
        });
    }

    @NotNull
    private static byte[] getBytesFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }

    @NotNull
    private static HttpServerConfig createConfigFromPort(final int port, final int threadCount) {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.deferAccept = true;
        acceptor.reusePort = true;

        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptor};
        config.minWorkers = threadCount;
        config.maxWorkers = threadCount;
        config.selectors = 4;
        return config;
    }
}
