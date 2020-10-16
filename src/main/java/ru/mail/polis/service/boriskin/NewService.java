package ru.mail.polis.service.boriskin;

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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Поддержка следующего HTTP REST API протокола:
 * 1. HTTP GET /v0/entity?id="ID" -- получить данные по ключу.
 * Возвращает 200 OK и данные или 404 Not Found
 * 2. HTTP PUT /v0/entity?id="ID" -- создать/перезаписать (upsert) данные по ключу.
 * Возвращает 201 Created
 * 3. HTTP DELETE /v0/entity?id="ID" -- удалить данные по ключу.
 * Возвращает 202 Accepted
 *
 * @author makary
 */
public class NewService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(NewService.class);

    @NotNull
    private final ExecutorService executorService;

    @NotNull
    private final DAO dao;

    /**
     * Конструктор {@link NewService}.
     *
     * @param port порт
     * @param dao Дао
     * @param workers воркеры
     * @param queueSize размер очереди
     * @throws IOException возможна ошибка при неправильных параметрах
     */
    public NewService(
            final int port,
            @NotNull final DAO dao,
            final int workers,
            final int queueSize) throws IOException {
        super(getConfigFrom(port));

        assert 0 < workers;
        assert 0 < queueSize;

        this.dao = dao;
        this.executorService = new ThreadPoolExecutor(
                workers,
                workers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder()
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler((t, e) ->
                                logger.error("Ошибка в {}, возникла при обработке запроса", t, e))
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
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

    @NotNull
    private static byte[] toByteArray(@NotNull final ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return Response.EMPTY;
        }

        final byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        assert !byteBuffer.hasRemaining();
        return result;
    }

    /**
     * HTTP GET /v0/entity?id="ID" -- получает данные по ключу.
     *
     * @param id ключ
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        getting(
                key,
                httpSession);
    }

    /**
     * HTTP PUT /v0/entity?id="ID" -- создает/перезаписывает (upsert) данные по ключу.
     *
     * @param id ключ
     * @param request данные
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(
            @Param(value = "id", required = true) final String id,
            final Request request,
            @NotNull final HttpSession httpSession
    ) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        upserting(
                key,
                request,
                httpSession);
    }

    /**
     * HTTP DELETE /v0/entity?id="ID" - удаляет данные по ключу.
     *
     * @param id ключ
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession
    ) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(id.getBytes(StandardCharsets.UTF_8));
        removing(
                key,
                httpSession);
    }

    private void idValidation(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession) {
        if (id.isEmpty()) {
            try {
                httpSession.sendResponse(resp(Response.BAD_REQUEST));
            } catch (IOException ioException) {
                logger.error("Не плучается отправить запрос", ioException);
            }
        }
    }

    private void getting(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession) {
        executorService.execute(() -> {
            try {
                doGet(key, httpSession);
            } catch (IOException ioException) {
                logger.error("Ошибка в getting ", ioException);
            }
        });
    }

    private void doGet(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession) throws IOException {
        try {
            httpSession.sendResponse(
                    Response.ok(
                            toByteArray(dao.get(key)))
            );
        } catch (NoSuchElementException e) {
            httpSession.sendResponse(resp(Response.NOT_FOUND));
        } catch (IOException ioException) {
            logger.error("Ошибка в GET: {}", toByteArray(key));
            httpSession.sendResponse(resp(Response.INTERNAL_ERROR));
        }
    }

    private void upserting(
            @NotNull final ByteBuffer key,
            final Request request,
            @NotNull final HttpSession httpSession) {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        executorService.execute(() -> {
            try {
                doUpsert(
                        key,
                        httpSession,
                        value);
            } catch (IOException ioException) {
                logger.error("Ошибка в upserting ", ioException);
            }
        });
    }

    private void doUpsert(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession,
            final ByteBuffer value) throws IOException {
        try {
            dao.upsert(key, value);
            httpSession.sendResponse(resp(Response.CREATED));
        } catch (IOException ioException) {
            logger.error("Ошибка в PUT: {}, значение: {}",toByteArray(key), toByteArray(value));
            httpSession.sendResponse(resp(Response.INTERNAL_ERROR));
        }
    }

    private void removing(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession) {
        executorService.execute(() -> {
            try {
                doRemove(
                        key,
                        httpSession);
            } catch (IOException ioException) {
                logger.error("Ошибка в removing ", ioException);
            }
        });
    }

    private void doRemove(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession) throws IOException {
        try {
            dao.remove(key);
            httpSession.sendResponse(
                    resp(Response.ACCEPTED)
            );
        } catch (IOException ioException) {
            logger.error("Ошибка в DELETE: {}", toByteArray(key));
            httpSession.sendResponse(resp(Response.INTERNAL_ERROR));
        }
    }

    /**
     * Метод проверки: отвечает ли сервер.
     *
     * @param httpSession сессия
     */
    @Path("/v0/status")
    public void status(final HttpSession httpSession) {
        executorService.execute(() -> {
            try {
                httpSession.sendResponse(Response.ok("OK"));
            } catch (IOException ioException) {
                logger.error("Ошибка при ответе", ioException);
            }
        });
    }

    /**
     * Логирует непонятные запросы.
     *
     * @param request Запрос
     * @param httpSession Диалоговое состояние
     * @throws IOException Возможна проблема ввода-вывода
     */
    @Override
    public void handleDefault(
            final Request request,
            final HttpSession httpSession
    ) throws IOException {
        logger.error("Непонятный запрос: {}", request);
        httpSession.sendResponse(resp(Response.BAD_REQUEST));
    }

    private Response resp(final String response) {
        return new Response(response, Response.EMPTY);
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
