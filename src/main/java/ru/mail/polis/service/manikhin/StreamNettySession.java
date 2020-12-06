package ru.mail.polis.service.manikhin;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
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
import io.netty.handler.stream.ChunkedStream;
import ru.mail.polis.Record;
import ru.mail.polis.service.manikhin.utils.StreamUtils;
import java.util.Iterator;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class StreamNettySession {
    private final Iterator<Record> iterator;
    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;

    /**
     * Stream netty service session.
     *
     * @param iterator - record iterator from DAO storage
     * @param ctx - channel handler context
     * @param request - input http request
     * */
    public StreamNettySession(final Iterator<Record> iterator, final ChannelHandlerContext ctx,
                              final FullHttpRequest request) {
        this.iterator = iterator;
        this.ctx = ctx;
        this.request = request;
    }

    /**
     * Handle for running stream in session.
     * */
    public void startStream() {
        final HttpResponse response = new DefaultHttpResponse(
                HTTP_1_1, HttpResponseStatus.OK
        );

        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        ctx.writeAndFlush(response).isCancelled();
        stream();
    }

    private void stream() {
        while (iterator.hasNext()) {
            final Record record = iterator.next();
            final byte [] data = StreamUtils.formNettyChunk(record.getKey(), record.getValue());
            ChunkedStream chunk = new ChunkedStream(new ByteBufInputStream(Unpooled.copiedBuffer(data)));

            ctx.writeAndFlush(chunk).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        }

        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).isCancelled();

        if (!HttpUtil.isKeepAlive(request)) {
            ctx.close().isCancelled();
        }
    }
}
