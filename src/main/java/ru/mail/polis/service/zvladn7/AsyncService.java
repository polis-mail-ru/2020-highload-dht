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
import one.nio.server.AcceptorConfig;
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncService extends HttpServer implements Service {

    private static final String ERROR_SENDING_RESPONSE = "Error when sending response";
    private static final String ERROR_SERVICE_UNAVAILABLE = "Cannot send SERVICE_UNAVAILABLE response";
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);
    private static final int CACHE_SIZE = 15;

    private final DAO dao;
    private final Map<String, byte[]> cache = new LinkedHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService es;

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
                        final int queueSize) throws IOException {
        super(provideConfig(port));
        this.dao = dao;
        this.es = new ThreadPoolExecutor(amountOfWorkers, amountOfWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> log.error("Error when processing request in: {}", t, e)
                        ).build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    private static byte[] toBytes(final String str) {
        return Utf8.toBytes(str);
    }

    private static byte[] toBytes(final ByteBuffer value) {
        if (value.hasRemaining()) {
            final byte[] result = new byte[value.remaining()];
            value.get(result);

            return result;
        }

        return Response.EMPTY;
    }

    private void updateCache(final String key, final byte[] value) {
        if (cache.size() >= CACHE_SIZE) {
            final Iterator<Map.Entry<String, byte[]>> iterator = cache.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
        cache.put(key, value);
    }

    private static ByteBuffer wrapString(final String str) {
        return ByteBuffer.wrap(toBytes(str));
    }

    private static ByteBuffer wrapArray(final byte[] arr) {
        return ByteBuffer.wrap(arr);
    }

    private static void sendServiceUnavailableResponse(final HttpSession session) {
        log.error("Cannot complete request");
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
        } catch (IOException ex) {
            log.error(ERROR_SERVICE_UNAVAILABLE, ex);
        }
    }

    private static void sendEmptyIdResponse(final HttpSession session) throws IOException {
        log.error("Empty key was provided in DELETE method!");
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private static void sendInternalErrorResponse(final HttpSession session,
                                                  final String id,
                                                  final Exception e) throws IOException {
        log.error("Internal error. Can't insert or update value with key: {}", id, e);
        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }

    private static void process(final Processor processor) {
        try {
            processor.process();
        } catch (IOException e) {
            log.error(ERROR_SENDING_RESPONSE, e);
        }
    }

    private void processRequest(final Processor processor, final HttpSession session) {
        try {
            es.execute(() -> process(processor));
        } catch (RejectedExecutionException e) {
            sendServiceUnavailableResponse(session);
        }
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        log.error("Unsupported mapping request.\n Cannot understand it: {} {}",
                request.getMethodName(), request.getPath());
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    /**
     * Return status of the server instance.
     *
     * @param session - server session
     */
    @Path("/v0/status")
    public void status(final HttpSession session) {
        processRequest(() -> session.sendResponse(Response.ok("Status: OK")), session);
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
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(@Param(value = "id", required = true) final String id,
                    final HttpSession session) {
        processRequest(() -> {
            log.debug("GET request with mapping: /v0/entity and key={}", id);
            if (id.isEmpty()) {
                sendEmptyIdResponse(session);
                return;
            }
            byte[] body;
            lock.lock();
            try {
                body = cache.get(id);
            } finally {
                lock.unlock();
            }
            if (body == null) {
                final ByteBuffer key = wrapString(id);
                final ByteBuffer value;
                try {
                    value = dao.get(key);
                } catch (NoSuchElementException e) {
                    log.info("Value with key: {} was not found", id, e);
                    session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
                    return;
                } catch (IOException e) {
                    sendInternalErrorResponse(session, id, e);
                    return;
                }
                body = toBytes(value);
                final byte[] finalBody = body;
                lockAction(() -> updateCache(id, finalBody));
            }
            session.sendResponse(Response.ok(body));
        }, session);
    }

    /**
     * This method delete value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 202 if value is successfully deleted
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void remove(@Param(value = "id", required = true) final String id,
                       final HttpSession session) {
        processRequest(() -> {
            log.debug("DELETE request with mapping: /v0/entity and key={}", id);
            if (id.isEmpty()) {
                sendEmptyIdResponse(session);
                return;
            }
            final ByteBuffer key = wrapString(id);
            try {
                dao.remove(key);
                lockAction(() -> cache.remove(id));
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        }, session);
    }

    /**
     * This method insert or update value with provided id.
     * Async response can have different values which depend on the key or io errors.
     * Values:
     * 1. 201 if value is successfully inserted and created
     * 2. 400 if id is empty
     * 3. 500 if some io error was happened
     *
     * @param id - String
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void upsert(
            @Param(value = "id", required = true) final String id,
            final Request request,
            final HttpSession session) {
        processRequest(() -> {
            log.debug("PUT request with mapping: /v0/entity with: key={}, value={}",
                    id, new String(request.getBody(), StandardCharsets.UTF_8));
            if (id.isEmpty()) {
                sendEmptyIdResponse(session);
                return;
            }

            final ByteBuffer key = wrapString(id);
            final ByteBuffer value = wrapArray(request.getBody());
            try {
                dao.upsert(key, value);
                lockAction(() -> cache.computeIfPresent(id, (k, v) -> request.getBody()));
            } catch (IOException e) {
                sendInternalErrorResponse(session, id, e);
                return;
            }
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        }, session);
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

    private void lockAction(final Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
