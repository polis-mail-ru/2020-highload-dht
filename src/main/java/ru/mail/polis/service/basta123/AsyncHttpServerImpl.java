package ru.mail.polis.service.basta123;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class AsyncHttpServerImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(AsyncHttpServerImpl.class);
    private final DAO dao;
    private final ExecutorService execService;
    private static final String CANT_SEND_RESPONSE = "can't send response";
    private static final String ARRAY_IS_FULL = "array is full";
    private static final int QUEUE_SIZE = 1024;

    /**
     * AsyncHttpServerImpl.
     *
     * @param config     - has server's parametrs.
     * @param dao        - for interaction with RocksDB.
     * @param numWorkers - for executor service.
     */
    public AsyncHttpServerImpl(final HttpServerConfig config,
                               final DAO dao,
                               final int numWorkers) throws IOException {
        super(config);
        this.dao = dao;
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
    private void sendResponse(final HttpSession httpSession, final String resultCode) {
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
    public void getValueByKey(final @Param("id") String id,
                              final HttpSession httpSession) {
        try {
            execService.execute(() -> {
                if (id == null || id.isEmpty()) {
                    sendResponse(httpSession, Response.BAD_REQUEST);
                    return;
                }
                get(id, httpSession);
            });
        } catch (RejectedExecutionException e) {
            log.error(ARRAY_IS_FULL, e);
            sendResponse(httpSession, Response.INTERNAL_ERROR);
        }

    }

    private void get(final String id, final HttpSession httpSession) {
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);
        ByteBuffer valueByteBuffer;
        final byte[] valueBytes;
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
        ;
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
    public void putValueByKey(final @Param("id") String id,
                              final Request request,
                              final HttpSession httpSession) throws IOException {
        try {
            execService.execute(() -> {
                if (id == null || id.isEmpty()) {
                    sendResponse(httpSession, Response.BAD_REQUEST);
                    return;
                }
                put(id, request, httpSession);
            });
        } catch (RejectedExecutionException e) {
            log.error(ARRAY_IS_FULL, e);
            sendResponse(httpSession, Response.INTERNAL_ERROR);
        }
    }

    private void put(final String id,
                     final Request request,
                     final HttpSession httpSession)
    {
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        final byte[] valueByte = request.getBody();
        final ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueByte);

        try {
            dao.upsert(keyByteBuffer, valueByteBuffer);
            final Response responseCreated = new Response(Response.CREATED, Response.EMPTY);
            httpSession.sendResponse(responseCreated);
        } catch (IOException e) {
            log.error("upsert error", e);
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
    public void deleteValueByKey(final @Param("id") String id, final HttpSession httpSession) throws IOException {
        try {
            execService.execute(() -> {
                if (id == null || id.isEmpty()) {
                    sendResponse(httpSession, Response.BAD_REQUEST);
                    return;
                }
                delete(id, httpSession);
            });
        } catch (RejectedExecutionException e) {
            log.error(ARRAY_IS_FULL, e);
            sendResponse(httpSession, Response.INTERNAL_ERROR);
        }
    }

    private void delete(final String id, final HttpSession httpSession)
    {
        final byte[] keyBytes = id.getBytes(UTF_8);
        final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

        try {
            dao.remove(keyByteBuffer);
            httpSession.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException e) {
            log.error("can't remove value", e);
            sendResponse(httpSession, Response.INTERNAL_ERROR);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession httpSession) throws IOException {
        httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    @Override
    public synchronized void start() {
        super.start();
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
}
