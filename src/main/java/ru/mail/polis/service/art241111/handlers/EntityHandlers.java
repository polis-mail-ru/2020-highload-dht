package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.art241111.codes.DirectCode;
import ru.mail.polis.service.art241111.utils.ResponseHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.art241111.codes.CommandsCode.*;
import static ru.mail.polis.service.art241111.utils.ExtractId.extractId;

public class EntityHandlers {
    private final ResponseHelper responseHelper = new ResponseHelper();
    @NotNull
    private final DAO dao;
    private String id;

    public EntityHandlers(@NotNull DAO dao, HttpServer server) {
        this.dao = dao;
        setHandlers(server);
    }

    private void setHandlers(HttpServer server) {
        server.createContext(
                DirectCode.ENTITY.getCode(),
                new ErrorHandler(
                        http -> {
                            id = extractId(http.getRequestURI().getQuery());

                            switch (http.getRequestMethod()) {
                                case "GET":
                                    setGetHandler(http);
                                    break;
                                case "PUT":
                                    setPutHandler(http);
                                    break;
                                case "DELETE":
                                    setDeleteHandler(http);
                                    break;
                                default:
                                    setDefaultHandler(http);
                            }
                            http.close();
                        }
                )
        );
    }

    public void setGetHandler(HttpExchange http) throws IOException {
        final ByteBuffer getValue = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
        responseHelper.setResponse(getValue.array(), id, http);
    }

    public void setPutHandler(HttpExchange http) throws IOException {
        final int contentLength = Integer.parseInt(
                http.getRequestHeaders().getFirst(http.getHttpContext().getPath())
        );
        final byte[] putValue = new byte[contentLength];
        if(http.getRequestBody().read(putValue) != putValue.length){
            throw new IOException("Can not read at once");
        }

        dao.upsert(ByteBuffer.wrap(id.getBytes(UTF_8)), ByteBuffer.wrap(id.getBytes(UTF_8)));
        responseHelper.setResponse(DATA_IS_UPSET.getCode(), http);
    }

    public void setDeleteHandler(HttpExchange http) throws IOException {
        dao.remove(ByteBuffer.wrap(id.getBytes(UTF_8)));
        responseHelper.setResponse(DELETE_IS_GOOD.getCode(), http);
    }

    public void setDefaultHandler(HttpExchange http) throws IOException {
        responseHelper.setResponse(METHOD_NOT_ALLOWED.getCode(), http);
    }
}
