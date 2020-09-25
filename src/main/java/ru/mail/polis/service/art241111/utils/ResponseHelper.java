package ru.mail.polis.service.art241111.utils;

import com.sun.net.httpserver.HttpExchange;
import ru.mail.polis.service.art241111.codes.CommandsCode;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResponseHelper {
    /**
     * Send value with checking id.
     * @param byteBufferValue - Value of the byte buffer format.
     * @param id - Id to check. If it is not present, the value 404 is returned, otherwise 200.
     * @param http - Access to sending commands from the server.
     * @throws  IOException  if an I/O error occurs.
     */
    public void setResponse(final ByteBuffer byteBufferValue,
                            final String id,
                            final HttpExchange http) throws IOException {
        final int status = "".equals(id) ? CommandsCode.ERR_STATUS.getCode() : CommandsCode.GOOD_STATUS.getCode();

        final byte[] value = new byte[byteBufferValue.remaining()];
        byteBufferValue.get(value);

        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
    }

    /**
     * Sending values with the specified status.
     * @param status - The status that you want to convey.
     * @param value - The value that you want to convey.
     * @param http - Access to sending commands from the server.
     * @throws IOException if an I/O error occurs.
     */
    public void setResponse(final int status, final byte[] value, final HttpExchange http) throws IOException {
        http.sendResponseHeaders(status, value.length);
        http.getResponseBody().write(value);
    }

    /**
     * Sending specified status.
     * @param status - The status that you want to convey.
     * @param http - Access to sending commands from the server.
     * @throws IOException if an I/O error occurs.
     */
    public void setResponse(final int status, final HttpExchange http) throws IOException {
        http.sendResponseHeaders(status,0);
    }
}
