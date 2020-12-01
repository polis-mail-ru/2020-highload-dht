package ru.mail.polis.service.kate.moreva;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.net.Socket;
import one.nio.pool.PoolException;
import one.nio.util.Utf8;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;

public class StreamingHttpClient extends HttpClient {
    private static final String CONNECTION_HEADER = "Connection: ";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length: ";
    private static final String TRANSFER_HEADER = "Transfer-Encoding: ";
    private static final long serialVersionUID = Long.MAX_VALUE;
    private static final int BUFFER_SIZE = 10000;

    public StreamingHttpClient(final ConnectionString conn) {
        super(conn);
    }

    /**
     * Method invokes stream to work with chunks.
     * */
    public synchronized void invokeStream(
            final Request request,
            final StreamConsumer streamConsumer)
            throws InterruptedException, PoolException, IOException, HttpException {
        final int method = request.getMethod();
        final byte[] rawRequest = request.toBytes();
        StreamReader responseReader;

        Socket socket = borrowObject();
        boolean keepAlive = false;
        try {
            try {
                socket.setTimeout(timeout == 0 ? readTimeout : timeout);
                socket.writeFully(rawRequest, 0, rawRequest.length);
                responseReader = new StreamReader(socket, BUFFER_SIZE);
            } catch (SocketTimeoutException e) {
                destroyObject(socket);
                socket = createObject();
                socket.writeFully(rawRequest, 0, rawRequest.length);
                responseReader = new StreamReader(socket, BUFFER_SIZE);
            }
            final Response response = responseReader.readResponse(method);
            keepAlive = !"close".equalsIgnoreCase(response.getHeader(CONNECTION_HEADER));
            streamConsumer.consume(responseReader);
        } finally {
            if (keepAlive) {
                returnObject(socket);
            } else {
                invalidateObject(socket);
            }
        }
    }

    interface StreamConsumer {
        void consume(final StreamReader stream)
                throws IOException, InterruptedException, PoolException, HttpException;
    }

    static class StreamReader implements Iterator<byte[]> {

        private final Socket socket;
        private final byte[] buf;
        private byte[] chunk;
        private int length;
        private int pos;

        private boolean needRead;
        private boolean doneWithChunks;
        private boolean isAvailable;
        private Response response;

        StreamReader(final Socket socket, final int bufferSize)
                throws IOException {
            this.socket = socket;
            this.buf = new byte[bufferSize];
            this.length = socket.read(buf, 0, bufferSize, 0);
            isAvailable = false;
            needRead = true;
            chunk = null;
            doneWithChunks = false;
        }

        Response readResponse(final int method) throws IOException, HttpException {
            final String responseHeader = readLine();
            if (responseHeader.length() <= 9) {
                throw new HttpException("Invalid response header: " + responseHeader);
            }

            response = new Response(responseHeader.substring(9));
            while (!readLine().isEmpty()) {
                response.addHeader( readLine());
            }

            if (method != Request.METHOD_HEAD && response.getStatus() != 204) {
                final String contentLength = response.getHeader(CONTENT_LENGTH_HEADER);
                if (contentLength != null) {
                    final byte[] body = new byte[Integer.parseInt(contentLength)];
                    final int contentBytes = length - pos;
                    System.arraycopy(buf, pos, body, 0, contentBytes);
                    if (contentBytes < body.length) {
                        socket.readFully(body, contentBytes, body.length - contentBytes);
                    }
                    response.setBody(body);
                    return response;
                }
                if ("chunked".equalsIgnoreCase(response.getHeader(TRANSFER_HEADER))) {
                    isAvailable = true;
                } else {
                    throw new HttpException("Content-Length is not specified");
                }
            }
            return response;
        }

        public Response getResponse() {
            return response;
        }

        public boolean isNotAvailable() {
            return !isAvailable;
        }

        @Override
        public boolean hasNext() {
            if (!isAvailable) {
                return false;
            }
            try {
                if (needRead) {
                    needRead = false;
                    readSingleChunk();
                }
            } catch (IOException | HttpException e) {
                return false;
            }
            return chunk != null;
        }

        @Override
        public byte[] next() {
            needRead = true;
            final var result = chunk;
            chunk = null;
            return result;
        }

        private String readLine() throws IOException, HttpException {
            final byte[] buffer = this.buf;
            final int lineStart = this.pos;
            int position = lineStart;

            do {
                if (position == length) {
                    if (position >= buffer.length) {
                        throw new HttpException("Line too long");
                    }
                    length += socket.read(buffer, position, buffer.length - position, 0);
                }
            } while (buffer[position++] != '\n');

            this.pos = position;
            return Utf8.read(buffer, lineStart, position - lineStart - 2);
        }

        private void readSingleChunk() throws IOException, HttpException {
            if (!doneWithChunks) {
                final int chunkSize = Integer.parseInt(readLine(), 16);
                if (chunkSize == 0) {
                    readLine();
                    doneWithChunks = true;
                    return;
                }

                chunk = new byte[chunkSize];

                final int contentBytes = length - pos;
                if (contentBytes < chunkSize) {
                    System.arraycopy(buf, pos, chunk, 0, contentBytes);
                    socket.readFully(chunk, contentBytes, chunkSize - contentBytes);
                    pos = 0;
                    length = 0;
                } else {
                    System.arraycopy(buf, pos, chunk, 0, chunkSize);
                    pos += chunkSize;
                    if (pos + 128 >= buf.length) {
                        length -= pos;
                        System.arraycopy(buf, pos, buf, 0, length);
                        pos = 0;
                    }
                }
                readLine();
            }
        }
    }
}
