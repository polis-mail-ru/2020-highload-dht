package ru.mail.polis.service.zvladn7;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.Socket;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.zvladn7.topology.ServiceTopology;
import ru.mail.polis.service.zvladn7.topology.Topology;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncService extends HttpServer implements Service {

    private static final String ERROR_SENDING_RESPONSE = "Error when sending response";
    private static final String ERROR_SERVICE_UNAVAILABLE = "Cannot send SERVICE_UNAVAILABLE response";
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    private final ExecutorService es;
    private final ServiceHelper helper;
    private final Topology<String> topology;

    /**
     * Asynchronous server implementation.
     *
     * @param port            - server port
     * @param dao             - DAO implemenation
     * @param amountOfWorkers - amount of workers in executor service
     * @param queueSize       - queue size of requests in executor service
     */
    public AsyncService(final int port,
                        @NotNull final DAO dao,
                        final int amountOfWorkers,
                        final int queueSize,
                        @NotNull final Topology<String> topology) throws IOException {
        super(provideConfig(port));
        this.es = new ThreadPoolExecutor(amountOfWorkers, amountOfWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error when processing request in: {}", t, e)
                        ).build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.helper = new ServiceHelper(topology, dao, es);
        this.topology = topology;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        log.info("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Return status of the server instance.
     *
     * @return Response - OK if service is available
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok("Status: OK");
    }

    @Path("/v0/entities")
    @RequestMethod(Request.METHOD_GET)
    public void range(@Param(value = "start", required = true) final String start,
                          @Param(value = "end") final String end,
                          @NotNull final HttpSession session) {
        if (isInvalidRangeParameters(start, end)) {
            try {
                sendEmptyIdResponse(session, "RANGE-GET");
            } catch (IOException e) {
                log.error("Invalid parameters in RANGE-GET request, start={}, end={}", start, end, e);
            }
            return;
        }
        helper.processRange(start, end, session);
    }

    /**
     * This method get value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 200 OK. Also return body.
     * 2. 400 if id is empty
     * 3. 404 if value with id was not found
     * 4. 500 if some io error was happened
     *
     * @param id       - String
     * @param replicas - replication factor
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id,
                    @Param(value = "replicas") final String replicas,
                    final HttpSession session,
                    final Request request) {
        processRequest(
                replicasHolder -> helper.handleGet(id, request, replicasHolder),
                session,
                id,
                request.getMethodName(),
                replicas);
    }

    /**
     * This method delete value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 202 if value is successfully deleted
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id       - String
     * @param replicas - replication factor
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void remove(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       final HttpSession session,
                       final Request request) {
        processRequest(
                replicasHolder -> helper.handleDelete(id, request, replicasHolder),
                session,
                id,
                request.getMethodName(),
                replicas);
    }

    /**
     * This method insert or update value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 201 if value is successfully inserted and created
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id       - String
     * @param replicas - replication factor
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void upsert(
            @Param(value = "id", required = true) final String id,
            @Param(value = "replicas") final String replicas,
            final Request request,
            final HttpSession session) {
        processRequest(
                replicasHolder -> helper.handleUpsert(id, request, replicasHolder),
                session,
                id,
                request.getMethodName(),
                replicas);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        es.shutdown();
        try {
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error when trying to stop executor service");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public HttpSession createSession(Socket socket) {
        return new RecordsStreamingSession(socket, this);
    }

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    private static void sendServiceUnavailableResponse(final HttpSession session, final Exception e) {
        log.error("Cannot complete request", e);
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
        } catch (IOException ex) {
            log.error(ERROR_SERVICE_UNAVAILABLE, ex);
        }
    }

    private void processRequest(@NotNull final Processor processor,
                                @NotNull final HttpSession session,
                                @NotNull final String id,
                                @NotNull final String methodName,
                                final String replicas) {
        final ReplicasHolder replicasHolder = parseReplicasParameter(replicas);
        log.debug("{} request with mapping: /v0/entity with: key={}", methodName, id);
        log.debug("ack: {}, from: {}", replicasHolder.ack, replicasHolder.from);
        try {
            if (id.isEmpty()) {
                sendEmptyIdResponse(session, methodName);
                return;
            }
            if (isInvalidReplicationFactor(replicasHolder)) {
                sendInvalidRFResponse(session, replicasHolder);
                return;
            }
            respond(
                    session,
                    processor.process(replicasHolder)
            );
        } catch (RejectedExecutionException | IOException e) {
            log.error(ERROR_SENDING_RESPONSE, e);
            sendServiceUnavailableResponse(session, e);
        }
    }

    private void respond(@NotNull final HttpSession session,
                         @NotNull final CompletableFuture<Response> future) {
        if (future.whenComplete((r, t) -> {
            try {
                if (t == null) {
                    session.sendResponse(r);
                } else {
                    final String responseStatus;
                    if (t.getCause() instanceof IllegalStateException) {
                        responseStatus = Response.GATEWAY_TIMEOUT;
                    } else {
                        responseStatus = Response.INTERNAL_ERROR;
                    }
                    session.sendResponse(new Response(responseStatus, Response.EMPTY));
                }
            } catch (IOException e) {
                log.error("Cannot send response: {}", r, e);
            }
        }).isCancelled()) {
            log.error("Canceled request");
        }
    }

    private static boolean isInvalidReplicationFactor(@NotNull final ReplicasHolder replicasHolder) {
        return replicasHolder.ack == 0 || replicasHolder.ack > replicasHolder.from;
    }

    private static void sendEmptyIdResponse(final HttpSession session,
                                            final String methodName) throws IOException {
        log.info("Empty key was provided in {} method!", methodName);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private static void sendInvalidRFResponse(final HttpSession session,
                                              final ReplicasHolder replicasHolder) throws IOException {
        log.info("Invalid replication factor with ack = {}, from = {}", replicasHolder.ack, replicasHolder.from);
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private ReplicasHolder parseReplicasParameter(final String replicas) {
        if (replicas == null) {
            return new ReplicasHolder(topology.size() / ServiceTopology.VIRTUAL_NODES_PER_NODE);
        } else {
            return new ReplicasHolder(replicas);
        }
    }

    private static boolean isInvalidRangeParameters(@NotNull final String start, final String end) {
        return start.isEmpty() || (end != null && end.isEmpty());
    }
}
