package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpServer;
import one.nio.http.Request;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SendFileSession extends StreamingSession {
    public SendFileSession(@NotNull Socket socket, @NotNull HttpServer server) {
        super(socket, server);
    }

    public void sendFile(@NotNull final RandomAccessFile file,
                         @NotNull final String uri)
            throws IOException {
        final var req = new Request(Request.METHOD_PUT, uri, true);
        final var headerBytes = req.toBytes();
        socket.write(headerBytes, 0, headerBytes.length);
        socket.sendFile(file, 0, file.length());
    }
}
