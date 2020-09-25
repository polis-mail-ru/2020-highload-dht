package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.art241111.utils.ResponseHelper;

import java.io.IOException;
import java.util.NoSuchElementException;

import static ru.mail.polis.service.art241111.codes.CommandsCode.ERR_STATUS;
import static ru.mail.polis.service.art241111.codes.CommandsCode.METHOD_NOT_ALLOWED;


public class ErrorHandler implements HttpHandler {
    @NotNull
    private final HttpHandler delegate;
    @NotNull
    private final ResponseHelper responseHelper = new ResponseHelper();

    public ErrorHandler(@NotNull final HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            delegate.handle(exchange);
        } catch (NoSuchElementException e) {
            responseHelper.setResponse(ERR_STATUS.getCode(), exchange);
            exchange.close();
        } catch (IllegalArgumentException | IOException e) {
            responseHelper.setResponse(METHOD_NOT_ALLOWED.getCode(), exchange);
            exchange.close();
        }
    }
}
