package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

public class NettyInit extends ChannelInitializer<SocketChannel> {
    private final DAO dao;
    private final Topology nodes;
    private final int queueSize;
    private final int countOfWorkers;
    private final int timeout;

    NettyInit(@NotNull final DAO dao, @NotNull final Topology nodes, final int countOfWorkers,
              final int queueSize, final int timeout) {

        this.nodes = nodes;
        this.queueSize = queueSize;
        this.timeout = timeout;
        this.countOfWorkers = countOfWorkers;
        this.dao = dao;
    }

    @Override
    protected void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(1024 * 512));
        pipeline.addLast(new NettyRequests(dao, nodes, countOfWorkers, queueSize, timeout));
    }
}
