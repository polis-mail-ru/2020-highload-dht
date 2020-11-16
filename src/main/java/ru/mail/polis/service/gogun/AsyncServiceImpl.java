package ru.mail.polis.service.gogun;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.gogun.ServiceUtils.handleDel;
import static ru.mail.polis.service.gogun.ServiceUtils.handleGet;
import static ru.mail.polis.service.gogun.ServiceUtils.handlePut;
import static ru.mail.polis.service.gogun.ServiceUtils.requestForRepl;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    public static final Duration TIMEOUT = Duration.ofSeconds(1);
    public static final String PROXY_HEADER = "X-Proxy-For: ";
    public static final String TIMESTAMP_HEADER = "timestamp: ";
    public static final String ABSENT = "-1";
    public static final String REPLICA_FACTOR_PARAM = "replicas=";

    @NotNull
    private final DAO dao;
    private final Hashing<String> topology;
    private final ExecutorService executorService;
    @NotNull
    private final java.net.http.HttpClient client;

    /**
     * class that provides requests to lsm dao via http.
     *
     * @param port       - port
     * @param numWorkers - num of threads in executor service
     * @param queueSize  - thread queue size
     * @param dao        - key-value storage
     * @throws IOException - ioexception
     */
    public AsyncServiceImpl(final int port, final int numWorkers, final int queueSize,
                            @NotNull final DAO dao,
                            @NotNull final Hashing<String> topology) throws IOException {
        super(ServiceUtils.makeConfig(port, numWorkers));
        this.dao = dao;
        this.topology = topology;
        this.executorService = new ThreadPoolExecutor(numWorkers,
                numWorkers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("Worker_%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error {} in {}", e, t))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());

        final Executor clientExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("Client_%d")
                        .build());
        this.client =
                java.net.http.HttpClient.newBuilder()
                        .executor(clientExecutor)
                        .connectTimeout(TIMEOUT)
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .build();
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        try {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        } catch (IOException e) {
            log.error("Can't send response {}", request, e);
        }
    }

    /**
     * provide checking api is alive.
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    private void execute(final String id, final HttpSession session,
                         final Request request) throws RejectedExecutionException {
        executorService.execute(() -> {
            try {
                handleRequest(id, request, session);
            } catch (IOException e) {
                log.error("Error sending response", e);
            }
        });
    }

    private void handleRequest(final String id, final Request request,
                               final HttpSession session) throws IOException {
        log.debug("{} request with id: {}", request.getMethodName(), id);
        if (id.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (request.getHeader(PROXY_HEADER) != null) {
            ServiceUtils.selector(() -> handlePut(key, request, dao, log),
                    () -> handleGet(key, dao, log),
                    () -> handleDel(key, dao, log),
                    request.getMethod(),
                    session);
            return;
        }

        final ReplicasFactor replicasFactor;
        final String replicaFactor = request.getParameter(REPLICA_FACTOR_PARAM);
        try {
            if (replicaFactor == null) {
                replicasFactor = ReplicasFactor.quorum(topology.all().size());
            } else {
                replicasFactor = ReplicasFactor.quorum(replicaFactor);
            }
        } catch (IllegalArgumentException e) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        final Set<String> replNodes;
        try {
            replNodes = topology.primaryFor(key, replicasFactor.getFrom());
        } catch (InvalidParameterException e) {
            log.error("Wrong replica factor", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }

        if (replNodes.isEmpty()) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }

        final List<CompletableFuture<Response>> responsesFuture = new ArrayList<>(replicasFactor.getFrom());
        replNodes.forEach((node) -> responsesFuture.add(proxy(node, request)));
        Futures.atLeastAsync(replicasFactor.getAck(), responsesFuture).whenCompleteAsync((v, t) -> {
            try {
                if (v == null) {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                    return;
                }

                final ResponseMerger responseMerger = new ResponseMerger(v, replicasFactor.getAck());

                ServiceUtils.selector(responseMerger::mergePutResponses,
                        responseMerger::mergeGetResponses,
                        responseMerger::mergeDeleteResponses,
                        request.getMethod(), session);
            } catch (IOException e) {
                log.error("error sending response", e);
            }
        }, executorService).isCancelled();
    }

    private CompletableFuture<Response> proxy(final String node, final Request request) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (topology.isMe(node)) {
            return ServiceUtils.selector(
                    () -> handlePut(key, request, dao, log),
                    () -> handleGet(key, dao, log),
                    () -> handleDel(key, dao, log),
                    request.getMethod(),
                    executorService);
        }

        HttpRequest requestForReplica;
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                requestForReplica = requestForRepl(node, id)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_GET:
                requestForReplica = requestForRepl(node, id).GET()
                        .build();
                return client.sendAsync(requestForReplica, GetBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            case Request.METHOD_DELETE:
                requestForReplica = requestForRepl(node, id).DELETE()
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(HttpResponse::body, executorService);
            default:
                log.error("Wrong request method");
                throw new IllegalStateException("Wrong request method");
        }
    }

    /**
     * Provide put/del/get requests to dao.
     *
     * @param id      - key
     * @param session - session
     */
    @Path("/v0/entity")
    public void handleHttpPath(@Param(value = "id", required = true) @NotNull final String id,
                               final HttpSession session,
                               final Request request) {
        try {
            execute(id, session, request);
        } catch (RejectedExecutionException e) {
            ServiceUtils.sendServiceUnavailable(session, log);
        }
    }

    /**
     * Method provides range request.
     *
     * @param start   - start key
     * @param end     - end key
     * @param session - http session
     * @throws IOException - error
     */
    @Path("/v0/entities")
    @RequestMethod(Request.METHOD_GET)
    public void handleRangeRequest(@Param(value = "start", required = true) @NotNull final String start,
                                   @Param(value = "end") @NotNull final String end,
                                   final HttpSession session) throws IOException {
        if (start.isEmpty()) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        executorService.execute(() -> {
            try {
                final ByteBuffer startBytes = ServiceUtils.getBuffer(start.getBytes(UTF_8));
                final ByteBuffer endBytes = ServiceUtils.checkEndParam(end)
                        ? null
                        : ServiceUtils.getBuffer(end.getBytes(UTF_8));

                ((StreamingSession) session).setIterator(dao.range(startBytes, endBytes));
            } catch (IOException e) {
                log.error("Error sending response", e);
            }
        });
    }

    @Override
    public HttpSession createSession(final Socket socket) {
        return new StreamingSession(socket, this);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Cant stop executor service", e);
            Thread.currentThread().interrupt();
        }
    }
}
