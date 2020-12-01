package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

public class NettyInit extends ChannelInitializer<SocketChannel> {
    private final DAO dao;
    private final Topology nodes;
    private final int queueSize;
    private final int countOfWorkers;

    NettyInit(@NotNull final DAO dao, @NotNull final Topology nodes, final int countOfWorkers,
              final int queueSize, final int timeout) {
        Logger log = LoggerFactory.getLogger(NettyInit.class);
        log.debug(String.valueOf(timeout));
        this.nodes = nodes;
        this.queueSize = queueSize;
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
        pipeline.addLast(new NettyRequests(dao, nodes,  countOfWorkers, queueSize));
    }
}
