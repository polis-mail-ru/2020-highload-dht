package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.art241111.codes.DirectCode;
import ru.mail.polis.service.art241111.utils.ExtractId;
import ru.mail.polis.service.art241111.utils.ResponseHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.art241111.codes.CommandsCode.DATA_IS_UPSET;
import static ru.mail.polis.service.art241111.codes.CommandsCode.DELETE_IS_GOOD;
import static ru.mail.polis.service.art241111.codes.CommandsCode.METHOD_NOT_ALLOWED;

public class EntityHandlers {
    private final ResponseHelper responseHelper = new ResponseHelper();
    @NotNull
    private final DAO dao;
    private String id;

    public EntityHandlers(@NotNull final DAO dao,final HttpServer server) {
        this.dao = dao;
        setHandlers(server);
    }

    private void setHandlers(final HttpServer server) {
        server.createContext(
                DirectCode.ENTITY.getCode(),
                new ErrorHandler(
                        http -> {
                            id = new ExtractId()
                                    .extractId(http.getRequestURI().getQuery());

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
                                    break;
                            }
                            http.close();
                        }
                )
        );
    }

    private void setGetHandler(final HttpExchange http) throws IOException {
        final ByteBuffer getValue = dao.get(ByteBuffer.wrap(id.getBytes(UTF_8)));
        responseHelper.setResponse(getValue, id, http);
    }

    private void setPutHandler(final HttpExchange http) throws IOException {
       // Let's find out what size of the array we should
       final int contentLength = Integer.parseInt(
               http.getRequestHeaders().getFirst("Content-length")
       );

       // Create array to save data
       final byte[] putValue = new byte[contentLength];

       // If we are sent an empty array, the value will be -1,
       // so we need to add a check for this
       int length = http.getRequestBody().read(putValue);
       length = length == -1 ? 0 : length;

       // We check whether the size of the received array matches the
       // size that was specified during transmission
       if (length != putValue.length) {
           throw new IOException("Can not read at once");
       }

       // Write data to the DAO and send a successful code
       dao.upsert(ByteBuffer.wrap(id.getBytes(UTF_8)), ByteBuffer.wrap(putValue));
       responseHelper.setResponse(DATA_IS_UPSET.getCode(), http);
    }

    private void setDeleteHandler(final HttpExchange http) throws IOException {
        dao.remove(ByteBuffer.wrap(id.getBytes(UTF_8)));
        responseHelper.setResponse(DELETE_IS_GOOD.getCode(), http);
    }

    private void setDefaultHandler(final HttpExchange http) throws IOException {
        responseHelper.setResponse(METHOD_NOT_ALLOWED.getCode(), http);
    }
}
