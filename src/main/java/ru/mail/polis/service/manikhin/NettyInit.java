package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;

public class NettyInit extends ChannelInitializer<SocketChannel> {
    private final DAO dao;
    private final Topology nodes;
    private final int timeout;
    private final DefaultEventExecutorGroup executor;

    NettyInit(@NotNull final DAO dao, @NotNull final Topology nodes, final int countOfWorkers, final int timeout) {
        this.nodes = nodes;
        this.timeout = timeout;
        this.executor = new DefaultEventExecutorGroup(countOfWorkers);
        this.dao = dao;
    }

    @Override
    protected void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024 * 512));
        pipeline.addLast(executor, new NettyRequests(dao, nodes, timeout));
    }
}
