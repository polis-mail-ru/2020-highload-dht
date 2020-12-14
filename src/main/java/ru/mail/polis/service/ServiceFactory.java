package ru.mail.polis.service;

import one.nio.http.HttpServerConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.basta123.RendezvousTopology;
import ru.mail.polis.service.basta123.ReplicHttpServerImpl;
import ru.mail.polis.service.basta123.Topology;

import java.io.IOException;
import java.util.Set;

import static ru.mail.polis.service.basta123.Utils.getHttpServerConfig;

/**
 * Constructs {@link Service} instances.
 *
 * @author Vadim Tsesko
 */
public final class ServiceFactory {

    private static final long MAX_HEAP = 256 * 1024 * 1024;

    private ServiceFactory() {
        // Not supposed to be instantiated
    }

    /**
     * Construct a storage instance.
     *
     * @param port     port to bind HTTP server to
     * @param dao      DAO to store the data
     * @param topology a list of all cluster endpoints {@code http://<host>:<port>} (including this one)
     * @return a storage instance
     */
    @NotNull
    public static Service create(
            final int port,
            @NotNull final DAO dao,
            @NotNull final Set<String> topology) throws IOException {
        if (Runtime.getRuntime().maxMemory() > MAX_HEAP) {
            throw new IllegalStateException("The heap is too big. Consider setting Xmx.");
        }

        if (port <= 0 || 65536 <= port) {
            throw new IllegalArgumentException("Port out of range");
        }

        final Topology<String> modularTopology = new RendezvousTopology(  topology, "http://localhost:" + port);
        final HttpServerConfig httpServerConfig = getHttpServerConfig(port);
        return new ReplicHttpServerImpl(
                httpServerConfig,
                dao,
                Runtime.getRuntime().availableProcessors(),
                modularTopology);
    }
}
