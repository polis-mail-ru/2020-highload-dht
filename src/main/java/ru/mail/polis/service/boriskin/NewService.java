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
import java.util.concurrent.*;

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
                        .setUncaughtExceptionHandler((t, e) -> logger.error(
                                "Ошибка в {}, возникла при обработке запроса", t, e))
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
     * @return 200 OK и данные или 404 Not Found
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public void get(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(
                id.getBytes(
                        StandardCharsets.UTF_8
                )
        );
        getting(
                key,
                httpSession);
    }

    /**
     * HTTP PUT /v0/entity?id="ID" -- создает/перезаписывает (upsert) данные по ключу.
     *
     * @param id ключ
     * @param request данные
     * @return 201 Created
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public void put(
            @Param(value = "id", required = true) final String id,
            final Request request,
            @NotNull final HttpSession httpSession
    ) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(
                id.getBytes(
                        StandardCharsets.UTF_8
                )
        );
        upserting(
                key,
                request,
                httpSession);
    }

    /**
     * HTTP DELETE /v0/entity?id="ID" - удаляет данные по ключу.
     *
     * @param id ключ
     * @return 202 Accepted
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public void delete(
            @Param(value = "id", required = true) final String id,
            @NotNull final HttpSession httpSession
    ) {
        idValidation(id, httpSession);

        final ByteBuffer key = ByteBuffer.wrap(
                id.getBytes(
                        StandardCharsets.UTF_8
                )
        );
        removing(
                key,
                httpSession);
    }

    private void idValidation(
            @Param(value = "id", required = true) String id,
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
        final ByteBuffer[] value = new ByteBuffer[1];
        executorService.execute(() -> {
            try {
                try {
                    value[0] = dao.get(key);
                } catch (NoSuchElementException e) {
                    httpSession.sendResponse(
                            resp(Response.NOT_FOUND));
                } catch (IOException e) {
                    logger.error(
                            "Ошибка в GET: {}", toByteArray(key));
                    httpSession.sendResponse(
                            resp(Response.INTERNAL_ERROR));
                }
                httpSession.sendResponse(
                        Response.ok(
                                toByteArray(value[0]))
                );
            } catch (IOException ioException) {
                logger.error(
                        "Не получается отправить запрос", ioException);
            }
        });
    }

    private void upserting(
            @NotNull final ByteBuffer key,
            final Request request,
            @NotNull final HttpSession httpSession) {
        final ByteBuffer value = ByteBuffer.wrap(request.getBody());
        executorService.execute(() -> {
            try {
                try {
                    dao.upsert(key, value);
                } catch (IOException e) {
                    logger.error(
                            "Ошибка в PUT: {}, значение: {}",toByteArray(key), toByteArray(value));
                    httpSession.sendResponse(
                            resp(Response.INTERNAL_ERROR));
                }
                httpSession.sendResponse(
                        resp(Response.CREATED));
            } catch (IOException ioException) {
                logger.error(
                        "Не получается отправить запрос", ioException);
            }
        });
    }

    private void removing(
            @NotNull final ByteBuffer key,
            @NotNull final HttpSession httpSession) {
        executorService.execute(() -> {
            try {
                try {
                    dao.remove(key);
                } catch (IOException e) {
                    logger.error(
                            "Ошибка в DELETE: {}", toByteArray(key));
                    httpSession.sendResponse(
                            resp(Response.INTERNAL_ERROR));
                }

                httpSession.sendResponse(
                        resp(Response.ACCEPTED));
            } catch (IOException ex) {
                logger.error(
                        "Не получается отправить запрос", ex);
            }
        });
    }

    @Path("/v0/status")
    public void status(final HttpSession httpSession) {
        try {
            final Future<?> future = executorService.submit(() -> {
                try {
                    httpSession.sendResponse(
                            Response.ok("OK"));
                } catch (IOException ioException) {
                    logger.error(
                            "Ошибка при ответе", ioException);
                }
            });
            if (future.isCancelled()) {
                logger.error(
                        "Задача отменена");
            }
        } catch (Exception exception) {
            logger.error(
                    "Не получается добавить запрос в очередь на выполнение", exception);
            try {
                httpSession.sendResponse(new Response(Response.SERVICE_UNAVAILABLE));
            } catch (IOException ioException) {
                logger.error(
                        "Не получилось отправить сообщение об ошибке", ioException);
            }
        }
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
        logger.error(
                "Непонятный запрос: {}", request);
        httpSession.sendResponse(
                resp(Response.BAD_REQUEST));
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
            logger.error(
                    "Не получилось завершить Executor Service", interruptedException);
        }
    }
}
