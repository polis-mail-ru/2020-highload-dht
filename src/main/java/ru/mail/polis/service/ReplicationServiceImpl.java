package ru.mail.polis.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;
import static ru.mail.polis.service.ServiceImpl.getConfig;

public class ReplicationServiceImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ReplicationServiceImpl.class);
    private static final int CONNECTION_TIMEOUT = 1;

    private enum ErrorNames {
        NOT_FOUND_ERROR, IO_ERROR, QUEUE_LIMIT_ERROR, PROXY_ERROR, METHOD_NOT_ALLOWED, EMPTY
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.NOT_FOUND_ERROR, "Value not found"),
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.QUEUE_LIMIT_ERROR, "Queue is full"),
            entry(ErrorNames.PROXY_ERROR, "Error forwarding request via proxy"),
            entry(ErrorNames.METHOD_NOT_ALLOWED, "Method not allowed"),
            entry(ErrorNames.EMPTY, "Key can't be empty")
    );

    @NotNull
    private final ReplicationHandler handler;

    ReplicationServiceImpl(
            final int port,
            @NotNull final DAO dao,
            final int workerPoolSize,
            final int queueSize,
            @NotNull final Topology topology
    ) throws IOException {

        super(getConfig(port));
        final ExecutorService exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
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

}
