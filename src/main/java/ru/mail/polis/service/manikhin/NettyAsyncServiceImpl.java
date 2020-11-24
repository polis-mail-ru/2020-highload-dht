package ru.mail.polis.service.manikhin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Service;

public class NettyAsyncServiceImpl implements Service {
    private final DAO dao;
    private final int port;
    private final Topology nodes;
    private final int timeout;
    private final int queueSize;
    private ChannelFuture cf;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workersGroup;
    private final Logger log = LoggerFactory.getLogger(NettyAsyncServiceImpl.class);

    public NettyAsyncServiceImpl(final int port, @NotNull final DAO dao,
                                 @NotNull final Topology nodes, final int countOfWorkers,
                                 final int queueSize, final int timeout) {

        this.port = port;
        this.dao = dao;
        this.nodes = nodes;
        this.timeout = timeout;
        this.queueSize = queueSize;
        bossGroup = new NioEventLoopGroup(countOfWorkers);
        workersGroup = new NioEventLoopGroup(countOfWorkers);
    }

    @Override
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workersGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyInit(dao, nodes, queueSize, timeout))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            cf = serverBootstrap.bind(port).sync();

        } catch (InterruptedException error) {
            log.error("Interrupted error: ", error);
        }
    }

    @Override
    public synchronized void stop() {
        try {
            final Future<?> bossGroupFuture = bossGroup.shutdownGracefully();
            final Future<?> workersGroupFuture = workersGroup.shutdownGracefully();

            if (bossGroupFuture.isCancelled()) {
                log.error("bossGroup error!");
            }

            if (workersGroupFuture.isCancelled()) {
                log.error("workersGroup error!");
            }

            cf = cf.channel().closeFuture().sync();
        } catch (InterruptedException error) {
            log.error("Can't stop server! Error: ", error);
            Thread.currentThread().interrupt();
        }
    }
}
