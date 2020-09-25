package ru.mail.polis.service.art241111.utils;

import com.sun.net.httpserver.HttpExchange;
import ru.mail.polis.service.art241111.codes.CommandsCode;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResponseHelper {
    public void setResponse(ByteBuffer value2, String id, HttpExchange http) throws IOException {
        int status = id.equals("") ? CommandsCode.ERR_STATUS.getCode(): CommandsCode.GOOD_STATUS.getCode();

        byte[] value = new byte[value2.remaining()];
        value2.get(value);

        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
    }

    public void setResponse(int status, byte[] value, HttpExchange http) throws IOException {
        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
    }

    public void setResponse(int status, HttpExchange http) throws IOException {
        http.sendResponseHeaders(status,0);
    }
}
