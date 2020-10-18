package ru.mail.polis.service.mariarheon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.mariarheon.ByteBufferUtils;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    @NotNull
    private final DAO dao;
    private final ExecutorService service;
    private static final String RESP_ERR = "Response can't be sent: ";

    /**
     * Asynchronous Service Implementation.
     *
     * @param config - configuration.
     * @param dao - dao
     */
    public AsyncServiceImpl(final HttpServerConfig config,
                       @NotNull final DAO dao) throws IOException {
        super(config);
        this.dao = dao;

        final int workers = Runtime.getRuntime().availableProcessors();
        service = new ThreadPoolExecutor(workers, workers, 0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                    .setNameFormat("async_workers-%d")
                .build()
                );
    }

    /** Get/set/delete key-value entity.
     * @param key - record id.
     * @param session - session.
     * @param request - request.
     **/
    @Path("/v0/entity")
    @RequestMethod({METHOD_GET, METHOD_PUT, METHOD_DELETE})
    public void handleEntityRequest(final @Param(value = "id", required = true) String key,
                                    @NotNull final HttpSession session,
                                    final @Param("request") Request request) {
        try {
            service.execute(() -> {
                switch (request.getMethod()) {
                    case METHOD_GET:
                        get(key, session);
                        break;
                    case METHOD_PUT:
                        put(key, request, session);
                        break;
                    case METHOD_DELETE:
                        delete(key, session);
                        break;
                    default:
                        break;
                }
            });
        } catch (RejectedExecutionException ex) {
            logger.error("Error in ServiceImpl.get() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    private void get(final @Param(value = "id", required = true) String key,
        @NotNull final HttpSession session) {
            try {
                service.execute(() -> {
                    getInternal(key, session);
                });
            } catch (RejectedExecutionException ex) {
                logger.error("Error in ServiceImpl.get() method; internal error: ", ex);
                try {
                    session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
                } catch (IOException ex1) {
                    logger.error(RESP_ERR, session, ex1);
                }
            }
        }

    private void getInternal(final String key,
                             final HttpSession session) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.getInternal() method: key is empty");
                session.sendResponse(new ZeroResponse(Response.BAD_REQUEST));
                return;
            }
            final ByteBuffer response = dao.get(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)));
            session.sendResponse(Response.ok(ByteBufferUtils.toArray(response)));
        } catch (NoSuchElementException ex) {
            try {
                session.sendResponse(new ZeroResponse(Response.NOT_FOUND));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.getInternal() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    private void put(final @Param(value = "id", required = true) String key,
                    final @Param("request") Request request,
                    @NotNull final HttpSession session) {
        try {
            service.execute(() -> {
                putInternal(key, request, session);
            });
        } catch (RejectedExecutionException ex) {
            logger.error("Error in ServiceImpl.put() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    private void putInternal(final String key,
                             final Request request,
                             final HttpSession session) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.putInternal() method: key is empty");
                session.sendResponse(new ZeroResponse(Response.BAD_REQUEST));
                return;
            }
            dao.upsert(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)),
                    ByteBufferUtils.toByteBuffer(request.getBody()));
            session.sendResponse(new ZeroResponse(Response.CREATED));
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.putInternal() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    private void delete(final @Param(value = "id", required = true) String key,
                       @NotNull final HttpSession session) {
        try {
            service.execute(() -> {
                deleteInternal(key, session);
            });
        } catch (RejectedExecutionException ex) {
            logger.error("Error in ServiceImpl.delete() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    private void deleteInternal(final String key,
                             final HttpSession session) {
        try {
            if (key.isEmpty()) {
                logger.info("ServiceImpl.deleteInternal() method: key is empty");
                session.sendResponse(new ZeroResponse(Response.BAD_REQUEST));
                return;
            }
            dao.remove(ByteBufferUtils.toByteBuffer(key.getBytes(StandardCharsets.UTF_8)));
            session.sendResponse(new ZeroResponse(Response.ACCEPTED));
        } catch (IOException ex) {
            logger.error("Error in ServiceImpl.deleteInternal() method; internal error: ", ex);
            try {
                session.sendResponse(new ZeroResponse(Response.INTERNAL_ERROR));
            } catch (IOException ex1) {
                logger.error(RESP_ERR, session, ex1);
            }
        }
    }

    /**
     * Check status.
     *
     * @param session - session
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) {
        try {
            session.sendResponse(new ZeroResponse(Response.OK));
        } catch (IOException ex) {
            logger.error(RESP_ERR, session, ex);
        }
    }

    @Override
    public void handleDefault(@NotNull final Request request,
                              @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new ZeroResponse(Response.BAD_REQUEST));
    }

}
