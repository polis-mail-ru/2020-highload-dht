package ru.mail.polis.service.boriskin;

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
import ru.mail.polis.service.Service;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.service.boriskin.ReplicaWorker.PROXY_HEADER;

/**
 * Поддержка следующего расширенного HTTP REST API протокола:
 * 1. HTTP GET /v0/entity?id="ID"[&replicas=ack/from] -- получить данные по ключу "ID".
 * Возвращает:
 * 200 OK и данные,
 *      если ответили хотя бы ack из from реплик;
 * 404 Not Found,
 *      если ни одна из ack реплик, вернувших ответ,
 *      не содержит данные (либо данные удалены хотя бы на одной из ack ответивших реплик);
 * 504 Not Enough Replicas,
 *      если не получили 200/404 от ack реплик из всего множества from реплик.
 * 2. HTTP PUT /v0/entity?id="ID"[&replicas=ack/from] -- создать/перезаписать данные по ключу "ID".
 * Возвращает:
 * 201 Created,
 *      если хотя бы ack из from реплик подтвердили операцию;
 * 504 Not Enough Replicas,
 *      если не набралось ack подтверждений из всего множества from реплик.
 * 3. HTTP DELETE /v0/entity?id="ID"[&replicas=ack/from] -- удалить данные по ключу "ID".
 * Возвращает:
 * 202 Accepted,
 *      если хотя бы ack из from реплик подтвердили операцию;
 * 504 Not Enough Replicas,
 *      если не набралось ack подтверждений из всего множества from реплик.
 *
 * @author makary
 */
public class NewService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(NewService.class);

    @NotNull
    private final ExecutorService executorService;
    @NotNull
    private final Topology<String> topology;
    
    @NotNull
    private final ReplicaFactor defaultReplicaFactor;
    @NotNull
    private final ReplicaWorker replicaWorker;

    /**
     * Конструктор {@link NewService}.
     *
     * @param port порт
     * @param dao Дао
     * @param topology топология
     * @throws IOException возможна ошибка при неправильных параметрах
     */
    public NewService(final int port,
                      @NotNull final DAO dao,
                      @NotNull final Topology<String> topology) throws IOException {
        super(getConfigFrom(port));
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("worker-%d").build());
        this.topology = topology;
        this.defaultReplicaFactor = ReplicaFactor.from(topology.all().size());
        this.replicaWorker = new ReplicaWorker(
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors(),
                        new ThreadFactoryBuilder().setNameFormat("proxy-worker-%d").build()),
                dao,
                topology);
    }

    @NotNull
    private static HttpServerConfig getConfigFrom(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        acceptorConfig.deferAccept = true;
        acceptorConfig.reusePort = true;
        final HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[]{acceptorConfig};
        return httpServerConfig;
    }

    /**
     * HTTP GET /v0/entity?id="ID" -- получает данные по ключу.
     * HTTP PUT /v0/entity?id="ID" -- создает/перезаписывает данные по ключу.
     * HTTP DELETE /v0/entity?id="ID" - удаляет данные по ключу.
     *
     * @param id ключ
     * @param replicas реплики
     * @param httpSession сессия
     * @param request данные
     */
    @Path("/v0/entity")
    public void entity(
            @Param("id") final String id,
            @Param("replicas") final String replicas,
            final HttpSession httpSession,
            final Request request) {
        idValidation(id, httpSession);
        final ReplicaFactor replicationFactor;
        try {
            replicationFactor = replicas == null
                    ? defaultReplicaFactor : ReplicaFactor.from(replicas);
            final int nodeSetSize = topology.all().size();
            if (replicationFactor.getFrom() > nodeSetSize) {
                throw new IllegalArgumentException(
                        "Неправильный фактор репликации:"
                                + "[from = " + replicationFactor.getFrom() + "] > [ nodeSetSize = " + nodeSetSize);
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            resp(httpSession, Response.BAD_REQUEST);
            return;
        }
        runExecutorService(httpSession, request, replicationFactor);
    }

    private void idValidation(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession) {
        if (id == null || id.isEmpty()) {
            resp(httpSession, Response.BAD_REQUEST);
        }
    }

    /**
     * Метод проверки: отвечает ли сервер.
     *
     * @param httpSession сессия
     */
    @Path("/v0/status")
    public Response status(
            final HttpSession httpSession) {
        return Response.ok("OK");
    }

    /**
     * Логирует непонятные запросы.
     *
     * @param request Запрос
     * @param httpSession Диалоговое состояние
     */
    @Override
    public void handleDefault(
            final Request request,
            final HttpSession httpSession) {
        logger.error("Непонятный запрос: {}", request);
        resp(httpSession, Response.BAD_REQUEST);
    }

    private void runExecutorService(
            @NotNull final HttpSession httpSession,
            @NotNull final Request request,
            @NotNull final ReplicaFactor replicationFactor) {
        try {
            executorService.execute(() -> operation(httpSession, request, replicationFactor));
        } catch (RejectedExecutionException rejectedExecutionException) {
            logger.error("Ошибка, превышен допустимый размер очередди задач ", rejectedExecutionException);
            resp(httpSession, Response.SERVICE_UNAVAILABLE);
        }
    }

    private void operation(
            @NotNull final HttpSession httpSession,
            @NotNull final Request request,
            @NotNull final ReplicaFactor replicationFactor) {
        final boolean alreadyProxied = request.getHeader(PROXY_HEADER) != null;
//        switch (request.getMethod()) {
//            case Request.METHOD_GET:
//                resp(httpSession,
//                        replicaWorker.getting(
//                                new MetaInfoRequest(request, replicationFactor, alreadyProxied)));
//                break;
//            case Request.METHOD_PUT:
//                resp(httpSession,
//                        replicaWorker.upserting(
//                                new MetaInfoRequest(request, replicationFactor, alreadyProxied)));
//                break;
//            case Request.METHOD_DELETE:
//                resp(httpSession,
//                        replicaWorker.removing(
//                                new MetaInfoRequest(request, replicationFactor, alreadyProxied)));
//                break;
//            default:
//                resp(httpSession, Response.METHOD_NOT_ALLOWED);
//                break;
//        }
        resp(httpSession, replicaWorker.entity(
                new MetaInfoRequest(request, replicationFactor, alreadyProxied),
                request));
    }

    private void resp(
            @NotNull final HttpSession httpSession,
            @NotNull final String response) {
        try {
            httpSession.sendResponse(new Response(response, Response.EMPTY));
        } catch (IOException ioException) {
            logger.error("Ошибка при отправке ответа ({}) ", response, ioException);
        }
    }

    private void resp(
            @NotNull final HttpSession httpSession,
            @NotNull final Response response) {
        try {
            httpSession.sendResponse(response);
        } catch (IOException ioException) {
            logger.error("Ошибка при отправке ответа ({}) ", response, ioException);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            logger.error("Не получилось завершить Executor Service", interruptedException);
            Thread.currentThread().interrupt();
        }
    }
}
