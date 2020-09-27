package ru.mail.polis.service.boriskin;

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

/**
 * Поддержка следующего HTTP REST API протокола:
 * 1. HTTP GET /v0/entity?id=<ID> -- получить данные по ключу <ID>.
 * Возвращает 200 OK и данные или 404 Not Found
 * 2. HTTP PUT /v0/entity?id=<ID> -- создать/перезаписать (upsert) данные по ключу <ID>.
 * Возвращает 201 Created
 * 3. HTTP DELETE /v0/entity?id=<ID> -- удалить данные по ключу <ID>.
 * Возвращает 202 Accepted
 *
 * @author makary
 */
public class NewService extends HttpServer implements Service {
    private static final Logger logger = LoggerFactory.getLogger(NewService.class);

    private enum Operations {
        GETTING, UPSERTING, REMOVING
    }

    @NotNull
    private final DAO dao;

    public NewService(
            final int port,
            @NotNull final DAO dao
    ) throws IOException {
        super(getConfigFrom(port));
        this.dao = dao;
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
     * HTTP GET /v0/entity?id=<ID> -- получает данные по ключу <ID>
     *
     * @param id ключ
     * @return 200 OK и данные или 404 Not Found
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_GET)
    public Response get(
            @Param(value = "id", required = true) final String id
    ) {
        return
                id.isEmpty() ?
                        resp(Response.BAD_REQUEST) :
                        operation(Operations.GETTING, id, null);
    }

    /**
     * HTTP PUT /v0/entity?id=<ID> -- создает/перезаписывает (upsert) данные по ключу <ID>
     *
     * @param id ключ
     * @param request данные
     * @return 201 Created
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_PUT)
    public Response put(
            @Param(value = "id", required = true) final String id,
            final Request request
    ) {
        return
                id.isEmpty() ?
                        resp(Response.BAD_REQUEST) :
                        operation(Operations.UPSERTING, id, request);
    }

    /**
     * HTTP DELETE /v0/entity?id=<ID> -- удаляет данные по ключу <ID>
     *
     * @param id ключ
     * @return 202 Accepted
     */
    @Path("/v0/entity")
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(
            @Param(value = "id", required = true) final String id
    ) {
        return
                id.isEmpty() ?
                        resp(Response.BAD_REQUEST) :
                        operation(Operations.REMOVING, id, null);
    }

    private Response operation(
            final Operations operation,
            @Param(value = "id", required = true) final String id,
            final Request request
    ) {
        final ByteBuffer key = ByteBuffer.wrap(
                id.getBytes(
                        StandardCharsets.UTF_8
                )
        );
        final ByteBuffer value;

        try {
            switch (operation) {
                case GETTING:
                    value = dao.get(key);
                    return Response.ok(toByteArray(value));
                case UPSERTING:
                    value = ByteBuffer.wrap(request.getBody());
                    dao.upsert(key, value);
                    return resp(Response.CREATED);
                case REMOVING:
                    dao.remove(key);
                    return resp(Response.ACCEPTED);
                default:
                    return resp(Response.BAD_REQUEST);
            }
        } catch (NoSuchElementException noSuchElementException) {
            logger.error(
                    "Ошибка при выполнении операции {}  для {}: {}",
                    operation, id, noSuchElementException);
            return resp(Response.NOT_FOUND);
        } catch (IOException ex) {
            logger.error(
                    "Ошибка при выполнении операции {} для {}: {}",
                    operation, id, ex);
            return resp(Response.INTERNAL_ERROR);
        }
    }

    @Path("/v0/status")
    public Response status() {
        return Response.ok("OK");
    }

    /**
     * Логирует непонятные запросы
     *
     * @param request Запрос
     * @param httpSession Диалоговое состояние
     * @throws IOException Возможна проблема ввода-вывода
     */
    @Override
    public void handleDefault(
            final Request request,
            HttpSession httpSession
    ) throws IOException {
        logger.error("Непонятный запрос: {}", request);
        httpSession.sendResponse(resp(Response.BAD_REQUEST));
    }

    private Response resp(String response) {
        return new Response(response, Response.EMPTY);
    }
}
