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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    private static final String FORWARD_REQUEST_HEADER = "PROXY_HEADER";
    private static final int CONNECTION_TIMEOUT = 1;

    private enum ErrorNames {
        NOT_FOUND_ERROR, IO_ERROR, QUEUE_LIMIT_ERROR, PROXY_ERROR, METHOD_NOT_ALLOWED
    }

    private static final Map<ErrorNames, String> MESSAGE_MAP = Map.ofEntries(
            entry(ErrorNames.NOT_FOUND_ERROR, "Value not found"),
            entry(ErrorNames.IO_ERROR, "IO exception raised"),
            entry(ErrorNames.QUEUE_LIMIT_ERROR, "Queue is full"),
            entry(ErrorNames.PROXY_ERROR, "Error forwarding request via proxy"),
            entry(ErrorNames.METHOD_NOT_ALLOWED, "Method not allowed")
    );

    @NotNull
    private final ExecutorService exec;
    @NotNull
    private final Topology topology;
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
        exec = new ThreadPoolExecutor(workerPoolSize, workerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Worker {} fails running: {}", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.topology = topology;
        Map<String, HttpClient> nodesToClients = new HashMap<>();
        this.handler = new ReplicationHandler(dao, topology, nodesToClients);

        for (final String node : topology.getNodes()) {

            if (topology.isSelfId(node)) {
                continue;
            }
            final HttpClient client = HttpClient.newBuilder()
                    .executor(exec)
                    .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT))
                    .build();
            if (nodesToClients.put(node, client) != null) {
                throw new IllegalStateException("Found multiple nodes with same ID");
            }
        }
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
            @NotNull final HttpSession session
    ) throws IOException {

        if (id.isEmpty()) {
            session.sendError(
                    Response.BAD_REQUEST, "Identifier is required as parameter. Error handling request"
            );
            return;
        }

        final boolean isForwardedRequest = req.getHeader(FORWARD_REQUEST_HEADER) != null;

        if (isForwardedRequest || topology.getNodes().size() > 1) {
            handler.handle(isForwardedRequest, req, session);
        } else {
            final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
            executeAsync(req, key, session);
        }
    }

    /**
     * resolves async request processing in single-node cluster.
     *
     * @param req     - HTTP request
     * @param key     - String object to be processed as a key in terms of data storage design
     * @param session - ongoing session instance
     */
    private void executeAsync(@NotNull final Request req,
                              @NotNull final ByteBuffer key,
                              @NotNull final HttpSession session) throws IOException {
        switch (req.getMethod()) {
            case Request.METHOD_GET:
                runAsyncHandler(session, () -> handler.singleGet(key));
                break;
            case Request.METHOD_PUT:
                runAsyncHandler(session, () -> handler.singleUpsert(key, req.getBody()));
                break;
            case Request.METHOD_DELETE:
                runAsyncHandler(session, () -> handler.singleDelete(key));
                break;
            default:
                session.sendError(Response.METHOD_NOT_ALLOWED, MESSAGE_MAP.get(ErrorNames.METHOD_NOT_ALLOWED));
                break;
        }
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

    /**
     * switches request handling to async-featured process.
     *
     * @param session ongoing HTTP session
     * @param async   interface design object to enable async handler execution
     */
    private void runAsyncHandler(@NotNull final HttpSession session, final Runner async) {
        exec.execute(() -> {
            try {
                session.sendResponse(async.execute());
            } catch (IOException exc) {

                try {
                    session.sendError(Response.INTERNAL_ERROR, MESSAGE_MAP.get(ErrorNames.IO_ERROR));
                } catch (IOException e) {
                    log.error(MESSAGE_MAP.get(ErrorNames.IO_ERROR));
                }
            }
        });
    }
}
