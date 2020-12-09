package ru.mail.polis.service.manikhin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import ru.mail.polis.service.manikhin.utils.ServiceUtils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NettyAsyncServiceImpl implements Service {
    private final int port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workersGroup;
    private final Logger log = LoggerFactory.getLogger(NettyAsyncServiceImpl.class);
    private final ThreadPoolExecutor executor;
    private final ServiceUtils utils;
    private final ReplicasNettyRequests replicaHelper;
    private final int clusterSize;

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
        this.clusterSize = nodes.getNodes().size();
        this.executor = new ThreadPoolExecutor(countOfWorkers, countOfWorkers, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("async_worker-%d").setUncaughtExceptionHandler((t, e) ->
                        log.error("Error in {} when processing request", t, e)
                ).build(), new ThreadPoolExecutor.AbortPolicy());

        final HttpClient client = HttpClient.newBuilder().executor(executor)
                .connectTimeout(Duration.ofSeconds(timeout)).version(HttpClient.Version.HTTP_1_1).build();

        this.utils = new ServiceUtils(dao, executor);
        this.replicaHelper = new ReplicasNettyRequests(nodes, client, utils);

        this.bossGroup = new NioEventLoopGroup();
        this.workersGroup = new NioEventLoopGroup();
    }

    @Override
    public void start() throws InterruptedException {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workersGroup).channel(NioServerSocketChannel.class)
                .childHandler(new NettyInit(replicaHelper, utils, clusterSize))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        final ChannelFuture future = serverBootstrap.bind(port);

        try {
            future.sync().isCancelled();
        } catch (InterruptedException error) {
            log.error("Can't stop server! Error: ", error);
            bossGroup.shutdownGracefully().isCancelled();
            workersGroup.shutdownGracefully().isCancelled();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
            bossGroup.shutdownGracefully().isCancelled();
            workersGroup.shutdownGracefully().isCancelled();
        } catch (InterruptedException error) {
            log.error("Can't stop server! Error: ", error);
            Thread.currentThread().interrupt();
        }
    }
}
