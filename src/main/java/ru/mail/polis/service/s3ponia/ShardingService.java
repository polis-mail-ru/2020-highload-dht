package ru.mail.polis.service.s3ponia;

import one.nio.http.HttpClient;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.util.Proxy;
import ru.mail.polis.util.Utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static ru.mail.polis.util.Utility.TIME_HEADER;

public class ShardingService implements HttpEntityHandler {
    public static final String PROXY_HEADER = "X-Proxy-From";
    final AsyncService asyncService;
    final ShardingPolicy<ByteBuffer, String> policy;
    private final Map<String, HttpClient> urlToClient;

    /**
     * Creates a new {@link ShardingService} with given {@link AsyncService} and {@link ShardingPolicy}.
     * @param asyncService base service for handling entity
     * @param policy sharding policy
     */
    public ShardingService(@NotNull final AsyncService asyncService,
                           @NotNull final ShardingPolicy<ByteBuffer, String> policy) {
        this.asyncService = asyncService;
        this.policy = policy;
        this.urlToClient = Utility.urltoClientFromSet(this.policy.homeNode(), this.policy.all());
    }

    @Override
    public void entity(final String id,
                       final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        final var proxyHeader = Header.getHeader(PROXY_HEADER, request);
        final var key = Utility.byteBufferFromString(id);
        final var node = policy.getNode(key);
        if (proxyHeader != null || node.equals(policy.homeNode())) {
            asyncService.entity(id, replicas, request, session);
            return;
        }

        request.addHeader(TIME_HEADER + ": " + System.currentTimeMillis());
        try {
            session.sendResponse(Proxy.proxy(request, urlToClient.get(node)));
        } catch (ProxyException e) {
            session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }
    }

    @Override
    public void close() throws IOException {
        asyncService.close();

        for (final HttpClient client : this.urlToClient.values()) {
            client.clear();
        }
    }
}
