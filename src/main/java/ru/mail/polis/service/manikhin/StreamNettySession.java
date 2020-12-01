package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class StreamNettySession {
    private final Iterator<Record> iterator;
    private static final byte[] LF = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final Logger log = LoggerFactory.getLogger(StreamNettySession.class);
    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;

    public StreamNettySession(final Iterator<Record> iterator, final ChannelHandlerContext ctx,
                              final FullHttpRequest msg) {
        this.iterator = iterator;
        this.ctx = ctx;
        this.request = msg;
    }

    public void startStream() throws IOException {
        final HttpResponse response = new DefaultHttpResponse(
                HTTP_1_1, HttpResponseStatus.OK
        );

        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        ctx.writeAndFlush(response).isCancelled();
        stream();
    }

    private void stream() throws IOException {
        byte[] data;
        boolean handling = false;

        while (iterator.hasNext()) {
            final Record record = iterator.next();
            data = formFillChunk(record.getKey(), record.getValue());
            log.debug("loop data: " + data.length);

            ctx.write(data).addListener(ChannelFutureListener.CLOSE);

            if (!HttpUtil.isKeepAlive(request)) {
                ctx.close().isCancelled();
            }

            handling = true;
        }

        ctx.write(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);

        if (!handling) {
            throw new IOException("Out of order response");
        }
    }

    private byte[] formFillChunk(final ByteBuffer key, final ByteBuffer value) {
        final int dataLength = key.limit() + LF.length + value.limit();
        final byte[] hexLength = Integer.toHexString(dataLength)
                .getBytes(StandardCharsets.US_ASCII);
        final int chunkLength = dataLength + 2 * CRLF.length + hexLength.length;
        final ByteBuffer data = ByteBuffer.wrap(new byte[chunkLength]);

        data.put(hexLength);
        data.put(CRLF);
        data.put(key);
        data.put(LF);
        data.put(value);
        data.put(CRLF);

        return data.array();
    }
}
