package ru.mail.polis.service.kovalkov;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.kovalkov.utils.BufferConverter;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
    private final ExecutorService service;
    private final DAO dao;

    /**
     * Constructor
     *
     * @param config - service configuration.
     * @param dao - dao implementation.
     */
    public AsyncServiceImpl(final HttpServerConfig config, final DAO dao) throws IOException {
        super(config);
        this.dao = dao;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L, TimeUnit.SECONDS,
                                    new ArrayBlockingQueue<>(1024),
                                    new ThreadFactoryBuilder()
                                    .setUncaughtExceptionHandler((t, e) -> log.error("Error in async_worker-{}:",t,e))
                                    .setNameFormat("async_worker-%d")
                                    .build());
    }

    /**
     * Server configuration.
     *
     * @return - return HttpServerConfig
     */
    public static HttpServerConfig getConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.maxWorkers = Runtime.getRuntime().availableProcessors();
        httpServerConfig.queueTime = 10;
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    /**
     * Check status.
     *
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public void status(final HttpSession session) {
        final Future<?> future = service.submit(() -> {
            try {
                session.sendResponse(Response.ok(Response.OK));
            } catch (IOException e) {
                log.error("Error with send response in status",e);
            }
        });
        if (future.isCancelled()) log.error("Status. Task cancelled");
    }

    /**
     * Get value by key.
     *
     * @param id - key.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public void get(@Param("id") final String id, final HttpSession session) {
        final Future<?> future = service.submit(() -> {
            if (id == null || id.isEmpty()) {
                try {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException e) {
                    log.error("Get. IOException wen id is absent");
                }
            }
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
            ByteBuffer value;
            byte[] bytes;
            try {
                value = dao.get(key);
                bytes = BufferConverter.unfoldToBytes(value);
                session.sendResponse(Response.ok(bytes));
            } catch (IOException e) {
                log.error("Method get. IO exception. ", e);
                throw new RuntimeException(e);
            } catch (NoSuchElementException e) {
                log.error("Method get. Can't find value by this key ", e);
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException ioException) {
                    log.error("Method get. Id is empty. Can't send response:", e);
                }
            }
        });
        if (future.isCancelled()) log.error("Get. Task cancelled");
    }

    /**
     * Put key and value to LSM.
     *
     * @param id - key.
     * @param request - contains value in the body.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public void put(@Param("id") final String id, final Request request, final HttpSession session) {
        final Future<?> future = service.submit(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
                final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                dao.upsert(key,value);
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
            } catch (IOException e) {
                log.error("Method put. IO exception.", e);
                throw new RuntimeException(e);
            }
        });
        if (future.isCancelled()) log.error("Put. Task cancelled");
    }

    /**
     * Delete by key.
     *
     * @param id - key.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public void delete(@Param("id") final String id, final HttpSession session) {
        final Future<?> future = service.submit(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                }
                final ByteBuffer key = ByteBuffer.wrap(id.getBytes(UTF_8));
                dao.remove(key);
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                log.error("Method delete. IO exception. ", e);
                throw new RuntimeException(e);
            }
        });
        if (future.isCancelled()) log.error("Delete. Task cancelled");
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        service.shutdown();
        try {
            dao.close();
        } catch (IOException e) {
            log.error("Dao IO exception when try close: ", e);
            throw new RuntimeException(e);
        }
    }
}
