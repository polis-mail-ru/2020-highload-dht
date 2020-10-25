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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
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
        this.helper = new ServiceHelper(topology, dao);
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
                    final HttpSession session,
                    final Request request) {
        processRequest(() -> helper.handleGet(id, session, request), session);
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
                       final HttpSession session,
                       final Request request) {
        processRequest(() -> helper.handleDelete(id, session, request), session);
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
        processRequest(() -> helper.handleUpsert(id, request, session), session);
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

    private static HttpServerConfig provideConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        final HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    private static void sendServiceUnavailableResponse(final HttpSession session, final RejectedExecutionException e) {
        log.error("Cannot complete request", e);
        try {
            session.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
        } catch (IOException ex) {
            log.error(ERROR_SERVICE_UNAVAILABLE, ex);
        }
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
            sendServiceUnavailableResponse(session, e);
        }
    }
}
