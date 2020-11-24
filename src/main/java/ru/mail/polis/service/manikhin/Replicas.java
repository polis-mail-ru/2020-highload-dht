package ru.mail.polis.service.manikhin;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Replicas {
    private final int ack;
    private final int from;
    private final static Logger log = LoggerFactory.getLogger(Replicas.class);

    public Replicas(final int ack, final int from) {
        this.ack = ack;
        this.from = from;
    }

    public static Replicas quorum(final int count) {
        final int n = count / 2 + 1;
        return new Replicas(n, count);
    }

    /**
     * Parses the request to get the needed number of answers (ack) and nodes (from).
     * */
    public static Replicas parser(final String replicas) {
        final List<String> params = Arrays.asList(replicas.split("/"));
        if (params.size() != 2) {
            throw new IllegalArgumentException("Wrong Replica factor: " + replicas);
        }

        final int confirmation = Integer.parseInt(params.get(0));
        final int from = Integer.parseInt(params.get(1));

        return new Replicas(confirmation, from);
    }

    /**
     * Calculate replica factor value.
     *
     * @param replicas - input replicas
     * @param session - input http-session
     * @param defaultReplicaFactor - default replica factor
     * @param clusterSize - input nodes cluster size
     * @return ReplicaFactor value
     */
    public static Replicas replicaFactor(final String replicas,
                                         @NotNull final HttpSession session,
                                         final Replicas defaultReplicaFactor,
                                         final int clusterSize) throws IOException {
        Replicas replicaFactor = null;

        try {
            replicaFactor = replicas == null ? defaultReplicaFactor : parser(replicas);
            if (replicaFactor.ack < 1 || replicaFactor.from < replicaFactor.ack || replicaFactor.from > clusterSize) {
                throw new IllegalArgumentException("From is is very big");
            }
            return replicaFactor;
        } catch (IllegalArgumentException error) {
            session.sendError(Response.BAD_REQUEST, "Wrong ReplicaFactor");
        }

        return replicaFactor;
    }

    public static Replicas replicaNettyFactor(final List<String> replicas,
                                              final @NotNull ChannelHandlerContext ctx,
                                              final Replicas defaultReplicaFactor,
                                              final int clusterSize) throws IOException {
        Replicas replicaFactor = null;

        try {
            replicaFactor = replicas == null ? defaultReplicaFactor : parser(replicas.get(0));
            if (replicaFactor.ack < 1 || replicaFactor.from < replicaFactor.ack || replicaFactor.from > clusterSize) {
                throw new IllegalArgumentException("From is is very big");
            }
            return replicaFactor;
        } catch (IllegalArgumentException error) {
            sendResponse("Wrong ReplicaFactor".getBytes(StandardCharsets.UTF_8), ctx);
        }
        return replicaFactor;
    }

    private static void sendResponse(final @NotNull byte[] bytes,
                                     final @NotNull ChannelHandlerContext ctx) {

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                Unpooled.copiedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Something wrong with written some data.");
            }
        });
        return;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
