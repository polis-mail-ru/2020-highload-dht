package ru.mail.polis.service.manikhin;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Record;
import ru.mail.polis.service.manikhin.utils.StreamUtils;
import java.util.Iterator;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class StreamNettySession extends ChannelDuplexHandler {
    private final Iterator<Record> iterator;
    private final FullHttpRequest currentRequest;

    /**
     * Stream netty service session.
     *
     * @param iterator - record iterator from DAO storage
     * */
    public StreamNettySession(final Iterator<Record> iterator, @NotNull FullHttpRequest request) {
        this.iterator = iterator;
        this.currentRequest = request;
    }

    @Override
    public void channelActive(@NotNull final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        if (iterator != null && ctx.channel().isWritable()) {
            stream(ctx);
        }
    }

    @Override
    public void channelInactive(@NotNull final ChannelHandlerContext ctx){
        ctx.flush();
    }

    /**
     * Handle for running stream in session.
     *
     * @param ctx - channel handler context
     * */
    public void startStream(@NotNull final ChannelHandlerContext ctx) {
        final HttpResponse response = new DefaultHttpResponse(
                HTTP_1_1, HttpResponseStatus.OK
        );

        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        ctx.writeAndFlush(response).isCancelled();
        stream(ctx);
    }

    private void stream(@NotNull final ChannelHandlerContext ctx) {
        while (iterator.hasNext() && ctx.channel().isWritable()) {
            final Record record = iterator.next();
            final byte [] data = StreamUtils.formNettyChunk(record.getKey(), record.getValue());
            final ChunkedStream chunk = new ChunkedStream(new ByteBufInputStream(Unpooled.copiedBuffer(data)));

            ctx.writeAndFlush(chunk).isCancelled();
        }

        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).isCancelled();

        if(!HttpUtil.isKeepAlive(currentRequest)) ctx.channel().close().isCancelled();
    }
}
