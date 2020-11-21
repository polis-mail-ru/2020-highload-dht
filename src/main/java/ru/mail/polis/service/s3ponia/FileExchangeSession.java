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
    private static final int MAX_HEADERS = 48;
    private FileChannel receiveFileChannel = null;
    private int maxFileSize = 0;
    
    public FileExchangeSession(@NotNull Socket socket, @NotNull HttpServer server) {
        super(socket, server);
    }
    
    public static synchronized void sendFile(@NotNull final Path path,
                                      @NotNull final SocketPool socketPool)
            throws IOException, InterruptedException, PoolException {
        final var request = new Request(Request.METHOD_PUT, "/v0/sync", false);
        final var file = path.toFile();
        final var randomAccessFile = new RandomAccessFile(file, "r");
        final var size = randomAccessFile.length();
        request.addHeader("Content-Length: " + size);
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
    protected int processHttpBuffer(byte[] buffer, int length) throws IOException, HttpException {
        int lineStart = 0; // Current position in the buffer
        
        if ((parsing != null && parsing.getBody() != null) || receiveFileChannel != null) { // Resume consuming request
            // body
            if (receiveFileChannel != null) {
                int remaining = Math.min(length, maxFileSize - requestBodyOffset);
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
            } else {
                byte[] body = parsing.getBody();
                int remaining = Math.min(length, body.length - requestBodyOffset);
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
            }
        }
        
        for (int i = lineStart; i < length; i++) {
            if (buffer[i] != '\n') continue;
            
            int lineLength = i - lineStart;
            if (i > 0 && buffer[i - 1] == '\r') lineLength--;
            
            // Skip '\n'
            i++;
            
            if (parsing == null) {
                parsing = parseRequest(buffer, lineStart, lineLength);
            } else if (lineLength > 0) {
                if (parsing.getHeaderCount() < MAX_HEADERS) {
                    parsing.addHeader(Utf8.read(buffer, lineStart, lineLength));
                }
                if (receiveFileChannel == null &&
                            parsing.getHeaderCount() == 2 && parsing.getHeader(FILE_HEADER + ": ") != null) {
                    final var file = Path.of("/home/geodesia", parsing.getHeader(FILE_HEADER + ": "));
                    if (!Files.exists(file)) {
                        Files.createFile(file);
                    }
                    receiveFileChannel = FileChannel.open(file, StandardOpenOption.WRITE);
                    maxFileSize = Integer.parseInt(parsing.getHeader("Content-Length: "));
                }
            } else if (receiveFileChannel == null) {
                // Empty line -- there is next request or body of the current request
                String contentLengthHeader = parsing.getHeader("Content-Length: ");
                if (contentLengthHeader != null) {
                    i += startParsingRequestBody(contentLengthHeader, buffer, i, length - i);
                    if (requestBodyOffset < parsing.getBody().length) {
                        // The body has not been read completely yet
                        return i;
                    }
                }
                
                // Process current request
                if (closing) {
                    return i;
                } else {
                    receiveFileChannel = null;
                    handleParsedRequest();
                }
            }
            
            lineStart = i;
        }
        
        return lineStart;
    }
}
