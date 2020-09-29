package ru.mail.polis.service.Basta123;

import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {

    public static ByteBuffer getByteBufferFromByteArray(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer;
    }

    public static byte[] getByteArrayFromByteBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        buffer.clear();
        return bytes;

    }


    public static HttpServerConfig getHttpServerConfig(int port) {
        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;

        HttpServerConfig httpServerConfig = new HttpServerConfig();
        httpServerConfig.acceptors = new AcceptorConfig[1];
        httpServerConfig.acceptors[0] = acceptorConfig;
        return httpServerConfig;
    }

}
