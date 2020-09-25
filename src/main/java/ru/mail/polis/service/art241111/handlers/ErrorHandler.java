package ru.mail.polis.service.art241111.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.art241111.utils.ResponseHelper;

import java.io.IOException;
import java.util.NoSuchElementException;

import static ru.mail.polis.service.art241111.codes.CommandsCode.*;

public class ErrorHandler implements HttpHandler {
    @NotNull
    private final HttpHandler delegate;
    @NotNull
    private final ResponseHelper responseHelper = new ResponseHelper();

    public ErrorHandler(@NotNull HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try{
            delegate.handle(exchange);
        } catch (NoSuchElementException e){
            responseHelper.setResponse(ERR_STATUS.getCode(), exchange);
            exchange.close();
        } catch (IllegalArgumentException e){
            responseHelper.setResponse(EMPTY_ID.getCode(), exchange);
            exchange.close();
        } catch (IOException e){
            responseHelper.setResponse(DATA_IS_UPSET.getCode(), exchange);
            exchange.close();
        }
    }
}
