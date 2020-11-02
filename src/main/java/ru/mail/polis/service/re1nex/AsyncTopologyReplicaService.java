package ru.mail.polis.service.re1nex;

import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.re1nex.Topology;

import java.io.IOException;

/**
 * AsyncTopologyService provides asynchronous service with methods for work with shading systems with replicas.
 */
public class AsyncTopologyReplicaService extends BaseAsyncService {
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(AsyncTopologyReplicaService.class);
    @NotNull
    private final ReplicaInfo defaultReplicaInfo;

    /**
     * Service for concurrent work with requests.
     *
     * @param port         - Server port
     * @param dao          - DAO impl
     * @param workersCount - number workers in pool
     * @param queueSize    - size of task's queue
     */
    public AsyncTopologyReplicaService(final int port,
                                       @NotNull final DAO dao,
                                       final int workersCount,
                                       final int queueSize,
                                       @NotNull final Topology<String> topology) throws IOException {
        super(port, dao, workersCount, queueSize, topology, logger);
        assert workersCount > 0;
        assert queueSize > 0;
        this.defaultReplicaInfo = ReplicaInfo.of(topology.getUniqueSize());
    }

    /**
     * Provide requests for AsyncTopologyReplicaService.
     */
    @Path("/v0/entity")
    public void handleRequest(@Param(value = "id", required = true) final String id,
                              @Param("replicas") final String replicas,
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
                apiController.sendReplica(id, replicaInfo, session, request);
            } else {
                apiController.handleResponseLocal(id,
                        session,
                        request);
            }
        }, session);
    }
}
