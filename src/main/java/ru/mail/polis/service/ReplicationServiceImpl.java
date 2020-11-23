package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static ru.mail.polis.service.ServiceImpl.getConfig;

public class ReplicationServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ReplicationServiceImpl.class);
    private static final int CONNECTION_TIMEOUT = 1;
    private final ThreadPoolExecutor exec;

    private enum ErrorNames {
        NOT_FOUND_ERROR, IO_ERROR, QUEUE_LIMIT_ERROR, PROXY_ERROR, METHOD_NOT_ALLOWED, EMPTY,
        NOT_ALLOWED_METHOD_ERROR, REJECTED, NOT_ENOUGH_NODES, BAD_RANGE_PARAMS
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.NOT_FOUND_ERROR, "Value not found"),
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.QUEUE_LIMIT_ERROR, "Queue is full"),
            entry(ErrorNames.PROXY_ERROR, "Error forwarding request via proxy"),
            entry(ErrorNames.METHOD_NOT_ALLOWED, "Method not allowed"),
            entry(ErrorNames.EMPTY, "Key can't be empty"),
            entry(ErrorNames.REJECTED, "RejectedExecutionException in range request"),
            entry(ErrorNames.NOT_ENOUGH_NODES, "Not enough nodes in cluster"),
            entry(ErrorNames.BAD_RANGE_PARAMS, "Bad range request parameters")
    );

    @NotNull
    private final ReplicationHandler handler;
    private final DAO dao;

    ReplicationServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final int workerPoolSize,
            final int queueSize,
            @NotNull final Topology topology
    ) throws IOException {

        super(getConfig(port));
        this.exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        final HttpClient client = HttpClient.newBuilder()
                .executor(exec)
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT))
                .build();
        this.handler = new ReplicationHandler(dao, topology, exec, client);
        this.dao = dao;
    }

    /**
     * handles formation request to inform client the server is alive and ready to exchange.
     *
     * @param session - ongoing session instance
     */
    @Path("/v0/status")
    public void status(@NotNull final HttpSession session) throws IOException {
        session.sendResponse(Response.ok("Server is running..."));
    }

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StreamSession(socket, this);
    }

    /**
     * Handles range requests from "start" to "end".
     *
     * @param startId - search from
     * @param endId   - search to
     * @param session - session
     */
    @Path("/v0/entities")
    public void entities(
            @Param(value = "start", required = true) final String startId,
            @Param(value = "end") final String endId,
            final HttpSession session
    ) {
        if (startId.isEmpty() || ((endId != null) && endId.isEmpty())) {
            log.error(MESSAGE_MAP.get(ErrorNames.BAD_RANGE_PARAMS));
            trySendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY), session);
            return;
        }

        final ByteBuffer start = ByteBuffer.wrap(startId.getBytes(UTF_8));
        final ByteBuffer end = (endId == null) ? null
                : ByteBuffer.wrap(endId.getBytes(UTF_8));

        try {
            exec.execute(() -> makeRangeRequest(session, start, end));
        } catch (RejectedExecutionException e) {
            trySendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY), session);
            log.error(MESSAGE_MAP.get(ErrorNames.REJECTED));
        }
    }

    private void makeRangeRequest(final HttpSession session, final ByteBuffer from, final ByteBuffer to) {
        try {
            final Iterator<Record> records = dao.range(from, to);
            ((StreamSession) session).setIterator(records);
        } catch (IOException e) {
            trySendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY), session);
            log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), e);
        }
    }

    private void trySendResponse(final Response response, final HttpSession session) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR), e);
        }
    }

    /**
     * resolves request handling by HTTP REST methods, provides any client with response (incl. server outcome code).
     *
     * @param id      - String object to be processed as a key in terms of data storage design
     * @param req     - HTTP request
     * @param session - ongoing session instance
     */
    @Path("/v0/entity")
    public void entity(
            @Param(value = "id", required = true) final String id,
            @NotNull final Request req,
            @NotNull final HttpSession session,
            @Param("replicas") final String replicas
    ) throws IOException {

        if (id.isEmpty()) {
            session.sendError(
                    Response.BAD_REQUEST, MESSAGE_MAP.get(ErrorNames.EMPTY)
            );
            return;
        }

        handler.handle(req, session, id, replicas);
    }

    /**
     * handler determined to run by default.
     *
     * @param req     - HTTP request
     * @param session - ongoing session instance
     */
    @Override
    public void handleDefault(@NotNull final Request req, @NotNull final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        exec.shutdown();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Couldn't terminate executor");
            Thread.currentThread().interrupt();
        }
    }

    private static HttpServerConfig getConfig(final int port) {
        if (port <= 1024 || port >= 65536) {
            throw new IllegalArgumentException("Invalid port");
        }
        final AcceptorConfig acc = new AcceptorConfig();
        final HttpServerConfig config = new HttpServerConfig();
        acc.port = port;
        acc.deferAccept = true;
        acc.reusePort = true;
        config.acceptors = new AcceptorConfig[]{acc};
        return config;
    }
}
