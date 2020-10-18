package ru.mail.polis.service.basta123;

import com.google.common.net.HttpHeaders;
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
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.basta123.Utils.getByteArrayFromByteBuffer;
import static ru.mail.polis.service.basta123.Utils.getByteBufferFromByteArray;

public class MyAsyncHttpServerImpl extends HttpServer implements Service {

    private static final Logger log = LoggerFactory.getLogger(MyAsyncHttpServerImpl.class);
    private final DAO dao;
    private final ExecutorService execService;
    String cantSendResponse = "can't send response";

    /**
     * MyAsyncHttpServerImpl.
     *
     * @param config     - has server's parametrs.
     * @param dao        - for interaction with RocksDB.
     * @param numWorkers - for executor service.
     */
    public MyAsyncHttpServerImpl(final HttpServerConfig config,
                                 final DAO dao,
                                 final int numWorkers) throws IOException {
        super(config);
        this.dao = dao;
        assert 0 < numWorkers;
        execService = new ThreadPoolExecutor(numWorkers,
                numWorkers,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error in worker {}", t, e))
                        .setNameFormat("worker-%d")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());

    }

    /**
     * Checking status.
     *
     * @return - return code 200 OK.
     */
    @Path("/abracadabra")
    @RequestMethod(Request.METHOD_GET)
    public Response abracadabraCheckMethod() {
        return new Response(Response.BAD_REQUEST, Response.EMPTY);
    }

    @Path("/v0/status")
    @RequestMethod(Request.METHOD_GET)
    public Response statusCheckMethod(final HttpSession httpSession) {
        return new Response(Response.OK, new byte[0]);
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
        } catch (IOException ioException) {
            log.error(cantSendResponse, ioException);
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
        execService.execute(() -> {
                    if (id == null || id.isEmpty()) {
                        sendResponse(httpSession, Response.BAD_REQUEST);
                        return;
                    }

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
                        throw new RuntimeException("Error getting value :", e);

                    } catch (NoSuchElementException e) {
                        sendResponse(httpSession, Response.NOT_FOUND);
                    }
                }
        );
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
        execService.execute(() -> {
            if (id == null || id.isEmpty()) {
                try {
                    httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ioException) {
                    log.error(cantSendResponse, ioException);
                }

            }

            final byte[] keyBytes = id.getBytes(UTF_8);
            final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

            final byte[] valueByte = request.getBody();
            final ByteBuffer valueByteBuffer = getByteBufferFromByteArray(valueByte);

            try {
                dao.upsert(keyByteBuffer, valueByteBuffer);
                final Response responseCreated = new Response(Response.CREATED, Response.EMPTY);
                responseCreated.addHeader(HttpHeaders.CONTENT_TYPE + ": " + "text/plain");
                httpSession.sendResponse(responseCreated);
            } catch (IOException ioException) {
                log.error("upsert error", ioException);
            }
        });
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
        execService.execute(() -> {
            if (id == null || id.isEmpty()) {
                try {
                    httpSession.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
                } catch (IOException ioException) {
                    log.error(cantSendResponse, ioException);
                }
            }
            final byte[] keyBytes = id.getBytes(UTF_8);
            final ByteBuffer keyByteBuffer = getByteBufferFromByteArray(keyBytes);

            try {
                dao.remove(keyByteBuffer);
                httpSession.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (IOException ioException) {
                log.error("can't remove value", ioException);
            }
        });
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(new Response(Response.BAD_REQUEST, new byte[0]));
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
        } catch (IOException ioException) {
            log.error("can't close DB");
            throw new RuntimeException(ioException);
        }
    }
}
