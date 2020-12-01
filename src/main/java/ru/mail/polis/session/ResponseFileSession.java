package ru.mail.polis.session;

import one.nio.http.HttpServer;
import one.nio.http.Response;
import one.nio.net.Socket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class ResponseFileSession extends StreamingSession {
    public ResponseFileSession(@NotNull Socket socket, @NotNull HttpServer server) {
        super(socket, server);
    }
    
    /**
     * Sends file directly to socket with http's headers.
     * @param sendFile file's {@link Path}
     * @throws IOException rethrows from {@link Socket#sendFile}
     */
    public void responseFile(@NotNull final Path sendFile) throws IOException {
        final var file = sendFile.toFile();
        final var randomAccessFile = new RandomAccessFile(file, "r");

        final var response = new Response(Response.OK);
        final var size = randomAccessFile.length();
        response.addHeader("Content-Length: " + size);
        writeResponse(response, false);
        socket.sendFile(randomAccessFile, 0, size);
    }
}
