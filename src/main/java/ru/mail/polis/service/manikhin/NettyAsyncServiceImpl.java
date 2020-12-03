package ru.mail.polis.service.manikhin;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
    private final int countOfWorkers;
    private final int queueSize;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workersGroup;
    private final Logger log = LoggerFactory.getLogger(NettyAsyncServiceImpl.class);

    /**
     * Start endpoint for init netty service.
     *
     * @param port - service port
     * @param dao - storage interface
     * @param nodes - nodes list
     * @param countOfWorkers - count of workers
     * @param queueSize - queue size
     * @param timeout - init timeout for http clients
     */
    public NettyAsyncServiceImpl(final int port, @NotNull final DAO dao, @NotNull final Topology nodes,
                                 final int countOfWorkers, final int queueSize, final int timeout) {

        this.port = port;
        this.dao = dao;
        this.nodes = nodes;
        this.queueSize = queueSize;
        this.timeout = timeout;
        this.countOfWorkers = countOfWorkers;

        bossGroup = new NioEventLoopGroup();
        workersGroup = new NioEventLoopGroup();
    }

    @Override
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workersGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyInit(dao, nodes, countOfWorkers, queueSize, timeout))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverBootstrap.bind(port).sync().isSuccess();
        } catch (InterruptedException error) {
            log.error("Interrupted error: ", error);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        bossGroup.shutdownGracefully().isSuccess();
        workersGroup.shutdownGracefully().isSuccess();
    }
}
