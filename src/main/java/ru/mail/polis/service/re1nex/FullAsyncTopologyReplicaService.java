package ru.mail.polis.service.re1nex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.re1nex.Topology;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AsyncTopologyService provides asynchronous service with methods for work with shading systems with replicas
 * by asynchronous HTTPClient.
 */
public class FullAsyncTopologyReplicaService extends HttpServer implements Service {
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(FullAsyncTopologyReplicaService.class);
    @NotNull
    private final ExecutorService executor;
    @NotNull
    private final AsyncApiControllerImpl controller;
    @NotNull
    private final ReplicaInfo defaultReplicaInfo;

    /**
     * Asynchronous service for concurrent work with replicas for requests.
     *
     * @param port         - Server port
     * @param dao          - DAO impl
     * @param workersCount - number workers in pool
     * @param queueSize    - size of task's queue
     */
    public FullAsyncTopologyReplicaService(final int port,
                                           @NotNull final DAO dao,
                                           final int workersCount,
                                           final int queueSize,
                                           @NotNull final Topology<String> topology) throws IOException {
        super(provideConfig(port));
        assert workersCount > 0;
        assert queueSize > 0;
        this.executor = new ThreadPoolExecutor(
                workersCount,
                queueSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> logger.error("Error {} in thread {}", e, t))
                        .setNameFormat("worker_%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.controller = new AsyncApiControllerImpl(dao, topology, logger, executor);
        final int from = topology.getUniqueSize();
        final int ack = from / 2 + 1;
        this.defaultReplicaInfo = new ReplicaInfo(ack, from);
    }

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    /**
     * Provide service status.
     *
     * @return Response - status
     */
    @Path("/v0/status")
    public Response status() {
        return Response.ok(Response.OK);
    }

    /**
     * Provide requests for AsyncTopologyReplicaService.
     */
    @Path("/v0/entity")
    public void handleRequest(@Param(value = "id", required = true) final String id,
                              @Param("replicas") final String replicas,
                              @Param(value = "timestamp") final String timestamp,
                              @NotNull final HttpSession session,
                              @NotNull final Request request) {
        executeTask(() -> {
            if (id.isEmpty()) {
                logger.info("Id is empty!");
                ApiUtils.sendErrorResponse(session, Response.BAD_REQUEST, logger);
                return;
            }
            if (request.getHeader(ApiUtils.PROXY_FOR) == null) {
                final ReplicaInfo replicaInfo;
                if (replicas == null) {
                    replicaInfo = defaultReplicaInfo;
                } else {
                    try {
                        replicaInfo = ReplicaInfo.of(replicas);
                    } catch (IllegalArgumentException exception) {
                        logger.info("Wrong params replica", exception);
                        ApiUtils.sendResponse(session, new Response(Response.BAD_REQUEST, Response.EMPTY), logger);
                        return;
                    }
                }
                controller.sendReplica(id, replicaInfo, session, request);
            } else {
                controller.handleResponseLocal(id,
                        session,
                        request,
                        timestamp);
            }
        }, session);
    }

    @Override
    public void handleDefault(final Request request,
                              final HttpSession session) throws IOException {
        logger.info("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Can't shutdown execution");
            Thread.currentThread().interrupt();
        }
    }

    protected void executeTask(final Runnable task, final HttpSession session) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            logger.error("Execute failed!", e);
            ApiUtils.sendErrorResponse(session, Response.SERVICE_UNAVAILABLE, logger);
        }
    }
}
