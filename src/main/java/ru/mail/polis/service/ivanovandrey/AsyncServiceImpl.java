package ru.mail.polis.service.ivanovandrey;

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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.Converter;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {

    private static final String ERROR_MESSAGE = "Can't send response. Session {}";

    @NotNull
    private final DAO dao;
    private final ExecutorService service;
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    /**
     * Constructor.
     *
     * @param port - service configuration.
     * @param dao - dao implementation.
     */
    public AsyncServiceImpl(final int port,
                       @NotNull final DAO dao) throws IOException {
        super(createConfig(port));
        this.dao = dao;
        final int countOfWorkers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                        .setNameFormat("async_worker-%d")
                        .build());
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        ac.deferAccept = true;
        ac.reusePort = true;

        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.maxWorkers = Runtime.getRuntime().availableProcessors();
        httpServerConfig.queueTime = 10;
        httpServerConfig.acceptors = new AcceptorConfig[]{ac};
        return httpServerConfig;
    }

    /** Get data by key method.
     * @param id - id request.
     * @param session - session.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public void get(@NotNull @Param(value = "id", required = true) final String id,
                    final HttpSession session) {
        service.execute(() -> {
            try {
            if (id.isEmpty()) {
                session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                return;
                }
            final ByteBuffer key = strToByteBuffer(id, UTF_8);
            final ByteBuffer val;
            val = dao.get(key);
            session.sendResponse(Response.ok(Converter.fromByteBufferToByteArray(val)));
            } catch (NoSuchElementException e) {
                try {
                session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException i) {
                    log.error(ERROR_MESSAGE, session, e);
            }
        } catch (IOException e) {
                log.error("Error in get request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    /** Put/update data by key method.
     * @param id - id request.
     * @param request - data.
     * @param session - session.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public void put(@NotNull @Param(value = "id", required = true) final String id,
                    @NotNull @Param(value = "request", required = true) final Request request,
                    @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final ByteBuffer key = strToByteBuffer(id, UTF_8);
                final ByteBuffer value = ByteBuffer.wrap(request.getBody());
                dao.upsert(key, value);
                session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
                } catch (IOException e) {
                log.error("Error in delete request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    /** Delete data by key method.
     * @param id - id request.
     * @param session - session.
     **/
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public void delete(@NotNull @Param(value = "id", required = true) final String id,
                       @NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                if (id.isEmpty()) {
                    session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                    return;
                }
                final ByteBuffer key = strToByteBuffer(id, UTF_8);
                dao.remove(key);
                session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
                } catch (NoSuchElementException e) {
                try {
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                } catch (IOException i) {
                    log.error(ERROR_MESSAGE, session, e);
                }
            } catch (IOException e) {
                log.error("Error in delete request", e);
                try {
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                } catch (IOException ex) {
                    log.error(ERROR_MESSAGE, session, ex);
                }
            }
        });
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        service.execute(() -> {
            try {
                session.sendResponse(Response.ok("OK"));
            } catch (IOException e) {
                log.error(ERROR_MESSAGE, session, e);
            }
        });
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    public static ByteBuffer strToByteBuffer(final String msg, final Charset charset) {
        return ByteBuffer.wrap(msg.getBytes(charset));
    }
}
