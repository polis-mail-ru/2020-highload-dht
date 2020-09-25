package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpServer;
import ru.mail.polis.service.art241111.codes.DirectCode;
import ru.mail.polis.service.art241111.utils.ResponseHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.mail.polis.service.art241111.codes.CommandsCode.GOOD_STATUS;

public class StatusHandler {
    private final ResponseHelper responseHelper = new ResponseHelper();

    public StatusHandler(final HttpServer server) {
        setHandler(server);
    }

    private void setHandler(final HttpServer server) {
        server.createContext(
                DirectCode.STATUS.getCode(),
                new ErrorHandler(
                    http -> {
                        final String response = "ONLINE";
                        responseHelper.setResponse(GOOD_STATUS.getCode(), response.getBytes(UTF_8), http);
                        http.close();
                    })
        );
    }
}
