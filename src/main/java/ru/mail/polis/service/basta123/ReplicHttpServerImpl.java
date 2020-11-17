package ru.mail.polis.service.basta123;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

public class ReplicHttpServerImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(ReplicHttpServerImpl.class);
    private static final String HTTP_CLIENT_TIMEOUT = "?timeout=1000";
    private static final int QUEUE_SIZE = 1024;
    private static final String ERROR_LOG = "Error sending";

    public static final String IO_ERROR = "IOexception";
    public static final String FORWARD_REQ = "forward request";
    public static final String TIMEOUT_ERROR = "response time out";

    private final ExecutorService execService;
    private final Topology<String> topology;
    private final Map<String, HttpClient> clientAndNode;
    private final AckFrom ackFrom;
    private final HelperReplicHttpServerImpl helper;
    private final DAO dao;
    boolean RequestForward;

    /**
     * class const.
     *
     * @param config     - has server's parametrs.
     * @param dao        - for interaction with RocksDB.
     * @param numWorkers - for executor service.
     * @param topology - info about nodes.
     */
    public ReplicHttpServerImpl(final HttpServerConfig config,
                                @NotNull final DAO dao,
                                final int numWorkers,
                                @NotNull final Topology<String> topology) throws IOException {

        super(config);
        assert numWorkers > 0;
        assert QUEUE_SIZE > 0;
        this.topology = topology;
        this.dao = dao;
        this.clientAndNode = new HashMap<>();
        this.ackFrom = new AckFrom(topology);

        this.helper = new HelperReplicHttpServerImpl(dao, topology, clientAndNode);

        for (final String node : topology.getAllNodes()) {
            if (!topology.isLocal(node) && !this.clientAndNode.containsKey(node)) {
                final HttpClient client = new HttpClient(new ConnectionString(node + HTTP_CLIENT_TIMEOUT));
                this.clientAndNode.put(node, client);
            }
        }

        this.execService = new ThreadPoolExecutor(numWorkers,
                numWorkers,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_SIZE),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in worker {}", t, e))
                        .setNameFormat("worker-%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());

    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response statusCheckMethod() {
        return new Response(Response.OK, Response.EMPTY);
    }

    @Override
    public void handleDefault(final Request request, @NotNull final HttpSession httpSession) throws IOException {
        httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        execService.shutdown();
        try {
            execService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("can't shutdown execService: ", e);
            Thread.currentThread().interrupt();
        }
        for (final HttpClient client : clientAndNode.values()) {
            client.clear();
        }

    }

    /**
     * Get value by key.
     *
     * @param id - key.
     */
    @Path(value = "/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void getValueByKey(@Param(value = "id", required = true) final String id,
                              final Request request,
                              @NotNull final HttpSession httpSession) throws IOException {

        if (!isIdValid(id, httpSession)) {
            return;
        }

        if (request.getHeader(FORWARD_REQ) == null) {
            RequestForward = false;
        } else {
            RequestForward = true;
        }

        final String replicas = request.getParameter("replicas");
        AckFrom ackFromNew = null;
        try {
            if (replicas != null) {
                Pair<Integer, Integer> ackFromPair = Utils.ParseReplicas(replicas);
                ackFromNew = new AckFrom();
                ackFromNew.setAckValue(ackFromPair.getValue0());
                ackFromNew.setFromValue(ackFromPair.getValue1());
            } else {
                ackFromNew = this.ackFrom;
            }
        } catch (IllegalArgumentException exc) {
            httpSession.sendError(Response.BAD_REQUEST, "request cant be parsed");
        }

        if (topology.getSize() > 1) {
            AckFrom finalAckFrom = ackFromNew;
            executeAsync(httpSession, () -> helper.getFromReplicas(
                    id,
                    finalAckFrom,
                    RequestForward));

        } else {
            executeAsync(httpSession, () -> helper.get(id, request));
        }
    }

    /**
     * put value in the DB.
     *
     * @param id      - key.
     * @param request with value.
     * @throws IOException - possible IO exception.
     */

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void putValueByKey(@Param(value = "id", required = true) final String id,
                              final Request request,
                              @NotNull final HttpSession httpSession) throws IOException {
        if (!isIdValid(id, httpSession)) {
            return;
        }

        if (request.getHeader(FORWARD_REQ) == null) {
            RequestForward = false;
        } else {
            RequestForward = true;
        }

        final String replicas = request.getParameter("replicas");
        AckFrom ackFromNew = null;
        try {
            if (replicas != null) {
                Pair<Integer, Integer> ackFromPair = Utils.ParseReplicas(replicas);
                ackFromNew = new AckFrom();
                ackFromNew.setAckValue(ackFromPair.getValue0());
                ackFromNew.setFromValue(ackFromPair.getValue1());
            } else {
                ackFromNew = this.ackFrom;
            }
        } catch (IllegalArgumentException exc) {
            httpSession.sendError(Response.BAD_REQUEST, "request cant be parsed");
        }

        if (topology.getSize() > 1) {
            AckFrom finalAckFrom = ackFromNew;
            executeAsync(httpSession, () -> helper.upsertToReplicas(
                    id,
                    request.getBody(),
                    finalAckFrom,
                    RequestForward));
        } else {
            executeAsync(httpSession, () -> helper.put(id, request));
        }
    }

    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void deleteValueByKey(@Param(value = "id", required = true) final String id,
                                 final Request request,
                                 @NotNull final HttpSession httpSession) throws IOException {
        if (!isIdValid(id, httpSession)) {
            return;
        }

        if (request.getHeader(FORWARD_REQ) == null) {
            RequestForward = false;
        } else {
            RequestForward = true;
        }

        final String replicas = request.getParameter("replicas");
        AckFrom ackFromNew = null;
        try {
            if (replicas != null) {
                Pair<Integer, Integer> ackFromPair = Utils.ParseReplicas(replicas);
                ackFromNew = new AckFrom();
                ackFromNew.setAckValue(ackFromPair.getValue0());
                ackFromNew.setFromValue(ackFromPair.getValue1());
            } else {
                ackFromNew = this.ackFrom;
            }
        } catch (IllegalArgumentException exc) {
            httpSession.sendError(Response.BAD_REQUEST, "request cant be parsed");
        }

        if (topology.getSize() > 1) {
            AckFrom finalAckFrom = ackFromNew;
            executeAsync(httpSession, () -> helper.deleteFromReplicas(
                    id,
                    finalAckFrom,
                    RequestForward));
        } else {
            executeAsync(httpSession, () -> helper.delete(id, request));
        }
    }

    private void executeAsync(@NotNull final HttpSession httpSession,
                              @NotNull final Action action) {

        try {
            execService.execute(() -> {
                makeAction(action, httpSession);
            });
        } catch (RejectedExecutionException e) {
            log.error("array is full", e);
            HelperReplicHttpServerImpl.sendResponse(httpSession, Response.SERVICE_UNAVAILABLE);
        }

    }

    private void makeAction(@NotNull final Action action, @NotNull final HttpSession httpSession) {
        try {
            httpSession.sendResponse(action.act());
        } catch (IOException e) {
            log.error("act throws ex:", e);
            try {
                httpSession.sendError(Response.INTERNAL_ERROR, ERROR_LOG);
            } catch (IOException ex) {
                log.error(IO_ERROR);
            }
        }
    }

    @FunctionalInterface
    interface Action {
        Response act() throws IOException;
    }

    private boolean isIdValid(final String id,
                              @NotNull final HttpSession httpSession) {
        if (id.isEmpty()) {
            HelperReplicHttpServerImpl.sendResponse(httpSession, Response.BAD_REQUEST);
            return false;
        } else {
            return true;
        }
    }

    @Path("/v0/entities")
    @RequestMethod(Request.METHOD_GET)
    public void handleRangeRequest(@NotNull final Request req,
                                   @NotNull final HttpSession httpSession) throws IOException {

        final String start = req.getParameter("start=");
        final String end = req.getParameter("end=");

        if (start == null || start.isEmpty()) {
            httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            return;
        }

        final ByteBuffer endBuffer;
        if (end == null) {
            endBuffer = null;
        } else {
            endBuffer = ByteBuffer.wrap(end.getBytes(Charset.defaultCharset()));
        }
        final ByteBuffer startBuffer = ByteBuffer.wrap(start.getBytes(Charset.defaultCharset()));

        final Iterator<Record> records = dao.range(
                startBuffer,
                endBuffer);
        ((StreamingSessionChunks) httpSession).init(records);
    }

    @Override
    public HttpSession createSession(@NotNull final Socket socket) {
        return new StreamingSessionChunks(socket, this);
    }

}
