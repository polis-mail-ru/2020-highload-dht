package ru.mail.polis.service.valaubr;

import com.google.common.base.Charsets;
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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpService extends HttpServer implements Service {
    private final DAO dao;
    private final Logger logger = LoggerFactory.getLogger(HttpService.class);
    private final ExecutorService executor;
    private final String noIdAndResponse = "Empty id && response is dropped";

    /**
     * Constructor of the service.
     *
     * @param port - port of connection
     * @param base - object of storage
     * @throws IOException - exceptions
     */
    public HttpService(final int port,
                       @NotNull final DAO base,
                       final int threadPool,
                       final int queueSize) throws IOException {
        super(config(port));
        dao = base;
        executor = new ThreadPoolExecutor(threadPool, threadPool, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize));
    }

    private static HttpServerConfig config(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    private byte[] converterFromByteBuffer(@NotNull final ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } else {
            return Response.EMPTY;
        }
    }


    /**
     * Return status of server.
     *
     * @return 200 - ok
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }


    /**
     * Getting Entity by id.
     *
     * @param id - Entity id
     *           200 - ok
     *           400 - Empty id in param
     *           404 - No such element in dao
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(required = true, value = "id") @NotNull final String id,
                    @NotNull final HttpSession session) {
        executor.execute(() -> {
            if (id.strip().isEmpty()) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                } catch (IOException e) {
                    logger.error(noIdAndResponse, e);
                }
            }
            try {
                session.sendResponse(Response.ok(
                        converterFromByteBuffer(dao.get(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8))))));
            } catch (NoSuchElementException e) {
                logger.error("Record not exist by id = {}", id);
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Record not exist && response is dropped: " + ioException);
                }
            } catch (IOException e) {
                logger.error("Error when getting record", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Error when getting record && response is dropped: ", ioException);
                }
            }
        });
    }


    /**
     * Insertion entity dao by id.
     *
     * @param id - Entity id
     *           201 - Create entity
     *           400 - Empty id in param
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(@Param(required = true, value = "id") @NotNull final String id,
                    @NotNull final Request request, @NotNull final HttpSession session) {
        executor.execute(() -> {
            if (id.strip().isEmpty()) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                } catch (IOException e) {
                    logger.error(noIdAndResponse, e);
                }
            }
            try {
                dao.upsert(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)), ByteBuffer.wrap(request.getBody()));
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Error when putting record", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Put error && response is dropped:", ioException);
                }
            }
        });
    }

    /**
     * Deleting entity from dao by id.
     *
     * @param id - Entity id
     *           202 - Delete entity
     *           400 - Empty id in param
     *           500 - Internal error
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(@Param(required = true, value = "id") @NotNull final String id,
                       @NotNull final HttpSession session) {
        executor.execute(() -> {
            if (id.strip().isEmpty()) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                } catch (IOException e) {
                    logger.error(noIdAndResponse, e);
                }
            }
            try {
                dao.remove(ByteBuffer.wrap(id.getBytes(Charsets.UTF_8)));
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                logger.error("Error when deleting record", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ioException) {
                    logger.error("Error when deleting record && response is dropped", e);
                }
            }
        });
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        executor.execute(() -> {
            try {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            } catch (IOException e) {
                logger.error("handleDefault can`t send response", e);
            }
        });
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Executor don`t wanna stop!!!", e);
            Thread.currentThread().interrupt();
        }
    }
}
