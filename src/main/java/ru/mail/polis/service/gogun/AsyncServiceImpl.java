package ru.mail.polis.service.gogun;

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
import ru.mail.polis.dao.gogun.Value;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.gogun.Entry.toProxyResponse;
import static ru.mail.polis.service.gogun.ServiceUtils.requestForRepl;

public class AsyncServiceImpl extends HttpServer implements Service {
    private static final Logger log = LoggerFactory.getLogger(AsyncServiceImpl.class);
    public static final Duration TIMEOUT = Duration.ofSeconds(1);
    public static final String PROXY_HEADER = "X-Proxy-For: ";
    public static final String TIMESTAMP_HEADER = "timestamp: ";
    public static final String REPLICA_FACTOR_PARAM = "replicas=";

    @NotNull
    private final DAO dao;
    private final Hashing<String> topology;
    @NotNull
    private final ReplicasFactor replicasFactor;
    private final ExecutorService executorService;
    @NotNull
    private final HttpClient client;

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
                HttpClient.newBuilder()
                        .executor(clientExecutor)
                        .connectTimeout(TIMEOUT)
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
        this.replicasFactor = ReplicasFactor.quorum(topology.all().size());
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
            switch (request.getMethod()) {
                case Request.METHOD_PUT:
                    session.sendResponse(handlePut(key, request));
                    break;
                case Request.METHOD_DELETE:
                    session.sendResponse(handleDel(key));
                    break;
                case Request.METHOD_GET:
                    session.sendResponse(toProxyResponse(handleGet(key)));
                    break;
                default:
                    session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
                    break;
            }
            return;
        }

        final String replicaFactorParameter = request.getParameter(REPLICA_FACTOR_PARAM);
        final ReplicasFactor localReplicasFactor;
        try {
            if (replicaFactorParameter == null) {
                localReplicasFactor = this.replicasFactor;
            } else {
                localReplicasFactor = ReplicasFactor.parseReplicaFactor(replicaFactorParameter);
            }
        } catch (IllegalArgumentException e) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }
        final Set<String> replNodes;
        try {
            replNodes = topology.primaryFor(key, localReplicasFactor.getFrom());
        } catch (InvalidParameterException e) {
            log.error("Wrong replica factor", e);
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }

        if (replNodes.isEmpty()) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            return;
        }
        if (request.getMethod() == Request.METHOD_GET) {
            final List<CompletableFuture<Entry>> responsesFutureGet = replNodes.stream()
                    .map(node -> proxyGet(node, request))
                    .collect(Collectors.toList());
            Futures.atLeastAsync(localReplicasFactor.getAck(), responsesFutureGet).whenCompleteAsync((v, t) -> {
                try {
                    if (v == null) {
                        session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                        return;
                    }

                    final EntryMerger<Entry> entryMerger = new EntryMerger<>(v, localReplicasFactor.getAck());

                    session.sendResponse(entryMerger.mergeGetResponses());
                } catch (IOException e) {
                    log.error("error sending response", e);
                }
            }, executorService).isCancelled();
        } else {
            final List<CompletableFuture<Response>> responsesFuture = replNodes.stream()
                    .map(node -> proxyDeletePut(node, request))
                    .collect(Collectors.toList());
            Futures.atLeastAsync(localReplicasFactor.getAck(), responsesFuture).whenCompleteAsync((v, t) -> {
                try {
                    if (v == null) {
                        session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                        return;
                    }

                    final EntryMerger<Response> entryMerger = new EntryMerger<>(v, localReplicasFactor.getAck());

                    session.sendResponse(entryMerger.mergePutDeleteResponses(request));

                } catch (IOException e) {
                    log.error("error sending response", e);
                }
            }, executorService).isCancelled();
        }

    }

    private Response handlePut(@NotNull final ByteBuffer key, @NotNull final Request request) {
        try {
            dao.upsert(key, ServiceUtils.getBuffer(request.getBody()));
        } catch (IOException e) {
            log.error("Internal server error put", e);
            return new Response(Response.INTERNAL_ERROR);
        }
        return new Response(Response.CREATED, Response.EMPTY);
    }

    private Entry handleGet(@NotNull final ByteBuffer key) {
        final Value value;
        Entry entry;
        try {
            value = dao.getValue(key);
        } catch (IOException e) {
            log.error("Internal server error get", e);
            throw new IllegalStateException(e);
        } catch (NoSuchElementException e) {
            entry = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Status.ABSENT);
            return entry;
        }

        if (value.isTombstone()) {
            entry = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Status.REMOVED);
        } else {
            entry = new Entry(Entry.ABSENT, ServiceUtils.getArray(value.getData()), Status.PRESENT);
        }
        entry.setTimestamp(value.getTimestamp());

        return entry;
    }

    private Response handleDel(@NotNull final ByteBuffer key) {
        try {
            dao.remove(key);
        } catch (IOException e) {
            log.error("Internal server error del", e);
            return new Response(Response.INTERNAL_ERROR);
        }
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    private CompletableFuture<Entry> proxyGet(final String node, final Request request) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (topology.isMe(node)) {
            return CompletableFuture.supplyAsync(() -> handleGet(key), executorService);
        }

        final HttpRequest requestForReplica = requestForRepl(node, id).GET().build();
        return client.sendAsync(requestForReplica, GetBodyHandler.INSTANCE)
                .thenApplyAsync(HttpResponse::body, executorService);
    }

    private CompletableFuture<Response> proxyDeletePut(final String node, final Request request) {
        final String id = request.getParameter("id=");
        final ByteBuffer key = ServiceUtils.getBuffer(id.getBytes(UTF_8));

        if (topology.isMe(node)) {
            if (request.getMethod() == Request.METHOD_PUT) {
                return CompletableFuture.supplyAsync(() -> handlePut(key, request), executorService);
            } else {
                return CompletableFuture.supplyAsync(() -> handleDel(key), executorService);
            }
        }

        HttpRequest requestForReplica;
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                requestForReplica = requestForRepl(node, id)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(request.getBody()))
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync((s) -> new Response(Response.CREATED, Response.EMPTY), executorService);
            case Request.METHOD_DELETE:
                requestForReplica = requestForRepl(node, id)
                        .DELETE()
                        .build();
                return client.sendAsync(requestForReplica, PutDeleteBodyHandler.INSTANCE)
                        .thenApplyAsync(s -> new Response(Response.ACCEPTED, Response.EMPTY), executorService);
            default:
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
