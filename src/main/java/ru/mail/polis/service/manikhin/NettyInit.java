package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

public class NettyInit extends ChannelInitializer<SocketChannel> {
    private final ReplicasNettyRequests replicaHelper;
    private final int clusterSize;
    private final ServiceUtils utils;

    /**
     * Netty pipeline initialization.
     *
     * @param replicaHelper - helper for replicas
     * @param utils - service utils
     * @param clusterSize - count nodes in cluster
     */
    public NettyInit(@NotNull final ReplicasNettyRequests replicaHelper, @NotNull final ServiceUtils utils,
                     final int clusterSize) {
        this.replicaHelper = replicaHelper;
        this.utils = utils;
        this.clusterSize = clusterSize;
    }

    @Override
    protected void initChannel(final SocketChannel ch) {
        final ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(1024 * 512));
        pipeline.addLast(new NettyRequests(replicaHelper, utils, clusterSize));
    }
}
