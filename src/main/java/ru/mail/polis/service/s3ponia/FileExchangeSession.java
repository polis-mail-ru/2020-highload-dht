package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.Request;
import one.nio.net.Socket;
import one.nio.pool.PoolException;
import one.nio.pool.SocketPool;
import one.nio.util.Utf8;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileExchangeSession extends StreamingSession {
    private static final String FILE_HEADER = "FILE_NAME";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final int MAX_HEADERS = 48;
    private FileChannel receiveFileChannel;
    private int maxFileSize;

    public FileExchangeSession(@NotNull final Socket socket, @NotNull final HttpServer server) {
        super(socket, server);
    }

    /**
     * Sends file in Request.
     *
     * @param path       file's path
     * @param socketPool socket for sending
     * @throws IOException          rethrows from socketPool
     * @throws InterruptedException rethrows from socketPool
     * @throws PoolException        rethrows from socketPool
     */
    public static synchronized void sendFile(@NotNull final Path path,
                                             @NotNull final SocketPool socketPool)
            throws IOException, InterruptedException, PoolException {
        final var request = new Request(Request.METHOD_PUT, "/v0/sync", false);
        final var file = path.toFile();
        final var randomAccessFile = new RandomAccessFile(file, "r");
        final var size = randomAccessFile.length();
        request.addHeader(CONTENT_LENGTH_HEADER + ": " + size);
        request.addHeader(FILE_HEADER + ": " + file.getName());
        var socket = socketPool.borrowObject();
        final var sendBytes = request.toBytes();
        try {
            socket.write(sendBytes, 0, sendBytes.length);
            socket.sendFile(randomAccessFile, 0, size);
        } catch (IOException exception) {
            socketPool.destroyObject(socket);
            socket = socketPool.createObject();
            socket.write(sendBytes, 0, sendBytes.length);
            socket.sendFile(randomAccessFile, 0, size);
        }
    }

    @Override
    protected int processHttpBuffer(@NotNull final byte[] buffer, final int length) throws IOException, HttpException {
        int lineStart = 0; // Current position in the buffer

        if ((parsing != null && parsing.getBody() != null) || receiveFileChannel != null) { // Resume consuming request
            // body
            if (receiveFileChannel == null) {
                final byte[] body = parsing.getBody();
                final int remaining = Math.min(length, body.length - requestBodyOffset);
                System.arraycopy(buffer, 0, body, requestBodyOffset, remaining);
                requestBodyOffset += remaining;

                if (requestBodyOffset < body.length) {
                    // All the buffer copied to body, but that is not enough -- wait for next data
                    return length;
                } else if (closing) {
                    return remaining;
                }

                // Process current request
                handleParsedRequest();
                lineStart = remaining;
            } else {
                final int remaining = Math.min(length, maxFileSize - requestBodyOffset);
                receiveFileChannel.write(ByteBuffer.wrap(buffer, 0,
                        remaining));
                requestBodyOffset += remaining;

                if (requestBodyOffset < maxFileSize) {
                    // All the buffer copied to body, but that is not enough -- wait for next data
                    return length;
                } else if (closing) {
                    return remaining;
                }

                receiveFileChannel.close();
                receiveFileChannel = null;

                // Process current request
                handleParsedRequest();
                lineStart = remaining;
            }
        }

        int skip = 0;
        for (int i = lineStart; i < length; i = skip + 1) {
            skip = i;
            if (buffer[skip] != '\n') continue;

            int lineLength = skip - lineStart;
            if (skip > 0 && buffer[skip - 1] == '\r') lineLength--;

            // Skip '\n'
            skip++;

            if (parsing == null) {
                parsing = parseRequest(buffer, lineStart, lineLength);
            } else if (lineLength > 0) {
                if (parsing.getHeaderCount() < MAX_HEADERS) {
                    parsing.addHeader(Utf8.read(buffer, lineStart, lineLength));
                }
                if (receiveFileChannel == null
                        && parsing.getHeaderCount() == 2 && parsing.getHeader(FILE_HEADER + ": ") != null) {
                    final var file = Path.of("/home/geodesia", parsing.getHeader(FILE_HEADER + ": "));
                    if (!Files.exists(file)) {
                        Files.createFile(file);
                    }
                    receiveFileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
                    maxFileSize = Integer.parseInt(parsing.getHeader(CONTENT_LENGTH_HEADER + ": "));
                }
            } else if (receiveFileChannel == null) {
                // Empty line -- there is next request or body of the current request
                final String contentLengthHeader = parsing.getHeader(CONTENT_LENGTH_HEADER + ": ");
                if (contentLengthHeader != null) {
                    skip += startParsingRequestBody(contentLengthHeader, buffer, skip, length - skip);
                    if (requestBodyOffset < parsing.getBody().length) {
                        // The body has not been read completely yet
                        return skip;
                    }
                }

                // Process current request
                if (closing) {
                    return skip;
                } else {
                    receiveFileChannel = null;
                    handleParsedRequest();
                }
            }

            lineStart = skip;
        }

        return lineStart;
    }
}
