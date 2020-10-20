package ru.mail.polis.service.basta123;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.*;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class AsyncHttpServerImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(AsyncHttpServerImpl.class);
    private static final String CANT_SEND_RESPONSE = "can't send response";
    private static final String ARRAY_IS_FULL = "array is full";
    private static final int QUEUE_SIZE = 1024;
    @NotNull
    private final DAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final Map<String, HttpClient> ClientAndNode;
    @NotNull
    private final ExecutorService execService;

    /**
     * AsyncHttpServerImpl.
     *
     * @param config     - has server's parametrs.
     * @param dao        - for interaction with RocksDB.
     * @param numWorkers - for executor service.
     */
    public AsyncHttpServerImpl(final HttpServerConfig config,
                               final DAO dao,
                               final int numWorkers,
                               @NotNull final Topology<String> topology) throws IOException {

        super(config);
        this.dao = dao;
        this.topology = topology;
        this.ClientAndNode = new HashMap<>();

        for (final String node: topology.getAllNodes()) {
            if (!topology.isLocal(node) && !this.ClientAndNode.containsKey(node)) {
                final HttpClient client = new HttpClient(new ConnectionString(node + "?timeout=1000"));
                this.ClientAndNode.put(node, client);
            }
        }

        assert 0 < numWorkers;
        execService = new ThreadPoolExecutor(numWorkers,
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

    /**
     * Send response.
     *
     * @param httpSession - httpSession.
     * @param resultCode  - send resultCode to the user.
     */
    private void sendResponse(@NotNull final HttpSession httpSession, final String resultCode) {
        try {
            httpSession.sendResponse(new Response(resultCode, Response.EMPTY));
        } catch (IOException e) {
            log.error(CANT_SEND_RESPONSE, e);
        }
    }

    /**
     * Get value by key.
     *
     * @param id - key.
     */
    @Path(value = "/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void getValueByKey(final @Param(value = "id", required = true) String id,
                              final Request request,
                              final HttpSession httpSession) {
        if (!isIdValid(id, httpSession)) {
            return;
        }
        executeAsync(httpSession, () -> get(id, request, httpSession));

    }

    private void get(@NotNull final String id, final Request request, final HttpSession httpSession) throws IOException {

        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);
        ByteBuffer valueByteBuffer;
        final byte[] valueBytes;

        final String endNode = topology.getNode(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                valueByteBuffer = dao.get(keyByteBuffer);
                valueBytes = getByteArrayFromByteBuffer(valueByteBuffer);
                httpSession.sendResponse(new Response(Response.ok(valueBytes)));

            } catch (IOException e) {
                log.error("get error: ", e);
                sendResponse(httpSession, Response.INTERNAL_ERROR);

            } catch (NoSuchElementException e) {
                sendResponse(httpSession, Response.NOT_FOUND);
            }
        } else {
           final Response response = proxying(endNode, request);
            httpSession.sendResponse(response);
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
    public void putValueByKey(final @Param(value = "id", required = true) String id,
                              final Request request,
                              final HttpSession httpSession) throws IOException {
        if (!isIdValid(id, httpSession)) {
            return;
        }
        executeAsync(httpSession, () -> put(id, request, httpSession));
    }

    private void put(@NotNull final String id,
                     @NotNull final Request request,
                     final HttpSession httpSession) throws IOException {
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        final byte[] valueByte = request.getBody();
        final ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueByte);

        final String endNode = topology.getNode(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                dao.upsert(keyByteBuffer, valueByteBuffer);
                final Response responseCreated = new Response(Response.CREATED, Response.EMPTY);
                httpSession.sendResponse(responseCreated);
            } catch (IOException e) {
                log.error("upsert error: ", e);
            }
        } else {
            final Response response = proxying(endNode, request);
            httpSession.sendResponse(response);
        }

    }

    /**
     * delete value by key.
     *
     * @param id - key.
     * @throws IOException - possible IO exception.
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void deleteValueByKey(@NotNull final @Param(value = "id", required = true) String id,
                                 final Request request,
                                 final HttpSession httpSession) throws IOException {
        if (!isIdValid(id, httpSession)) {
            return;
        }
        executeAsync(httpSession, () -> delete(id, request, httpSession));

    }

    private void delete(@NotNull final String id,
                        final Request request,
                        @NotNull final HttpSession httpSession) throws IOException {
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);
        final String endNode = topology.getNode(keyByteBuffer);
        if (topology.isLocal(endNode)) {
            try {
                dao.remove(keyByteBuffer);
                httpSession.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException e) {
                log.error("can't remove value: ", e);
                sendResponse(httpSession, Response.INTERNAL_ERROR);
            }
        } else {
           final Response response = proxying(endNode, request);
           httpSession.sendResponse(response);
        }
    }

    private Response proxying(@NotNull final String node,
                              @NotNull final Request request) throws IOException {
        try {
            request.addHeader("Forwarding");
            return ClientAndNode.get(node).invoke(request);
        } catch (IOException | HttpException | InterruptedException | PoolException e) {
            log.error("error when proxying the request: ", e);
            return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }
    }

    private void executeAsync(@NotNull final HttpSession httpSession,
                              @NotNull final Action action) {

        try {
            execService.execute(() -> {
                makeAct(action);
            });
        } catch (RejectedExecutionException e) {
            log.error(ARRAY_IS_FULL, e);
            sendResponse(httpSession, Response.INTERNAL_ERROR);
        }

    }

    private boolean isIdValid(@NotNull final String id,
                              @NotNull final HttpSession httpSession) {
        if (id == null || id.isEmpty()) {
            sendResponse(httpSession, Response.BAD_REQUEST);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleDefault(final Request request, @NotNull final HttpSession httpSession) throws IOException {
        httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void stop() {
        execService.shutdown();
        super.stop();
        try {
            dao.close();
        } catch (IOException e) {
            log.error("can't close DB");
            throw new RuntimeException(e);
        }
    }

    private void makeAct(@NotNull final Action action) {
        try {
            action.act();
        } catch (IOException e) {
            log.error("act throws ex: ", e);
        }
    }

    @FunctionalInterface
    interface Action {
        void act() throws IOException;
    }

}
