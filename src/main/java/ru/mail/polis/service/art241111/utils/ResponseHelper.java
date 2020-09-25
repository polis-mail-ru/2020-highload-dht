package ru.mail.polis.service.art241111.utils;

import com.sun.net.httpserver.HttpExchange;
import ru.mail.polis.service.art241111.codes.CommandsCode;

import java.io.IOException;

public class ResponseHelper {
    public void setResponse(byte[] value, String id, HttpExchange http) throws IOException {
        int status = id.equals("") ? CommandsCode.ERR_STATUS.getCode(): CommandsCode.GOOD_STATUS.getCode();

        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
    }

    public void setResponse(int status, byte[] value, HttpExchange http) throws IOException {
        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
        http.close();
    }

    public void setResponse(int status, HttpExchange http) throws IOException {
        http.sendResponseHeaders(status,0);
        http.close();
    }
}
