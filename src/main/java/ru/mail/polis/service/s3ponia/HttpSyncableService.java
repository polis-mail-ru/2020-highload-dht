package ru.mail.polis.service.s3ponia;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.nio.http.HttpServer;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.Response;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.s3ponia.DiskTable;
import ru.mail.polis.session.ResponseFileSession;
import ru.mail.polis.session.StreamingSession;
import ru.mail.polis.util.Proxy;
import ru.mail.polis.util.Utility;
import ru.mail.polis.util.merkletree.MerkleTree;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpSyncableService extends HttpBasicService {
    private static final Logger logger = LoggerFactory.getLogger(HttpEntityService.class);
    private static final int RANGES_COUNT = 1024;
    private final HttpEntityEntitiesHandler httpSyncEntityEntitiesService;
    private final DaoService daoService;
    private final Set<String> nodes;
    private final AtomicInteger counter = new AtomicInteger();
    private final HttpClient httpClient;
    
    /**
     * Creates a new {@link HttpSyncableService} with given port and {@link HttpEntityHandler}.
     *
     * @param port                          listenable server's port
     * @param httpSyncEntityEntitiesService entity request handler
     * @param service                       service for communicating with dao
     * @param nodes                         nodes in cluster
     * @throws IOException rethrow from {@link HttpServer#HttpServer}
     */
    public HttpSyncableService(final int port,
                               @NotNull final HttpEntityEntitiesHandler httpSyncEntityEntitiesService,
                               @NotNull final DaoService service, Set<String> nodes) throws IOException {
        super(port);
        if (!Files.exists(rootDirectory())) {
            Files.createDirectory(rootDirectory());
        }
        this.daoService = service;
        this.nodes = nodes;
        BasicConfigurator.configure();
        this.httpSyncEntityEntitiesService = httpSyncEntityEntitiesService;
        final var executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder()
                        .setNameFormat("client-%d")
                        .build()
        );
        this.httpClient = HttpClient.newBuilder()
                                  .version(HttpClient.Version.HTTP_1_1)
                                  .connectTimeout(Duration.ofSeconds(2))
                                  .executor(executor)
                                  .build();
    }
    
    private java.nio.file.Path rootDirectory() {
        return Paths.get("" + port);
    }
    
    private java.nio.file.Path relativePath(final String s) {
        return Paths.get(rootDirectory().toAbsolutePath().toString(), s);
    }
    
    @Path("/v0/merkleTree")
    public void merkleTree(@NotNull final HttpSession session) throws IOException {
        final MerkleTree tree = daoService.snapshot().merkleTree(RANGES_COUNT);
        final var response = new Response(Response.OK);
        final var body = tree.body();
        response.addHeader("Content-Length: " + body.length);
        response.setBody(body);
        session.sendResponse(response);
    }
    
    @Path("/v0/syncRange")
    public void sync(@Param(value = "start", required = true) final String start,
                     @Param(value = "end", required = true) final String end,
                     @NotNull final Request request,
                     @NotNull final HttpSession session) throws IOException {
        if (request.getHeader(Proxy.PROXY_HEADER + ": ") == null) {
            session.sendError(Response.BAD_REQUEST, "No proxy header");
            return;
        }
        
        final var snapshot = daoService.snapshot();
        final var pathSave =
                relativePath(counter.incrementAndGet() +
                                     request.getHeader(Proxy.PROXY_HEADER + ": ").substring(7) + "-"
                                     + start + "-" + end + ".back");
        Files.createFile(pathSave);
        
        try {
            snapshot.saveTo(pathSave, Long.parseLong(start), Long.parseLong(end));
        } catch (NumberFormatException e) {
            logger.error("Error in long parsing", e);
            session.sendError(Response.BAD_REQUEST, "Error in long parsing");
            return;
        }
        final var sendSession = (ResponseFileSession) session;
        sendSession.responseFile(pathSave);
        Files.delete(pathSave);
    }
    
    private HttpRequest merkleRequest(@NotNull final String node) {
        try {
            return HttpRequest.newBuilder()
                           .uri(new URI(node + "/v0/merkleTree"))
                           .timeout(Duration.ofSeconds(1))
                           .GET()
                           .build();
        } catch (URISyntaxException e) {
            logger.error("Error in uri parsing", e);
            throw new RuntimeException("Error in URI parsing", e);
        }
    }
    
    private HttpRequest syncRangeRequest(@NotNull final String node,
                                         final long start,
                                         final long end) {
        try {
            return HttpRequest.newBuilder()
                           .header(Proxy.PROXY_HEADER, "http://localhost:" + port)
                           .uri(new URI(node + "/v0/syncRange?start=" + start + "&end=" + end))
                           .timeout(Duration.ofSeconds(1))
                           .GET()
                           .build();
        } catch (URISyntaxException e) {
            logger.error("Error in uri parsing", e);
            throw new RuntimeException("Error in URI parsing", e);
        }
    }
    
    private CompletableFuture<java.nio.file.Path> repair(@NotNull final String node,
                                                         @NotNull final MismatchedRanges.Range range,
                                                         @NotNull final java.nio.file.Path pathSave) {
        final var request = syncRangeRequest(node, range.start(), range.end());
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(pathSave))
                       .thenApply(r -> r.body());
    }
    
    @Path("/v0/repair")
    public void repair(@NotNull final HttpSession session) throws IOException {
        final var snapshot = daoService.snapshot();
        
        final var tree = snapshot.merkleTree(RANGES_COUNT);
        final var mismatchedNodes = new MismatchedRanges(tree);
        final var successCounter = new AtomicInteger(nodes.size() - 1);
        final var failuresCounter = new AtomicInteger(1);
        
        for (final var node :
                nodes) {
            if (node.equals("http://localhost:" + port)) {
                continue;
            }
            final var request = merkleRequest(node);
            final var nodeRangesMismatch =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApply(HttpResponse::body)
                            .thenApply(t -> {
                                final var merkleTree = new MerkleTree(t);
                                return mismatchedNodes.mismatchedNodes(merkleTree);
                            });
            if (nodeRangesMismatch.whenComplete((a, t) -> {
                if (t == null) {
                    final var successArrayCounter = new AtomicInteger(a.size());
                    a.forEach(r -> {
                        final var pathSave = relativePath(counter.incrementAndGet() + "_" + r.start() + "_" + r.end() +
                                                                  ".daoRange");
                        try {
                            Files.createFile(pathSave);
                        } catch (IOException e) {
                            logger.error("Error in creating file", e);
                        }
                        repair(node, r, pathSave).whenComplete((path, th) -> {
                            try {
                                if (th == null) {
                                    daoService.merge(DiskTable.of(path));
                                    if (successArrayCounter.decrementAndGet() == 0) {
                                        if (successCounter.decrementAndGet() == 0) {
                                            session.sendResponse(Response.ok(Response.EMPTY));
                                        }
                                    }
                                } else {
                                    if (failuresCounter.decrementAndGet() == 0) {
                                        logger.error("Error in repairing range = ({}; {})", r.start(), r.end(), th);
                                        session.sendError(Response.INTERNAL_ERROR, "Error in repairing range");
                                    }
                                }
                            } catch (IOException e) {
                                logger.error("Error in response sending", e);
                            }
                        });
                    });
                    if (a.isEmpty()) {
                        try {
                            session.sendResponse(new Response(Response.OK, Response.EMPTY));
                        } catch (IOException e) {
                            logger.error("Error in sending response", e);
                        }
                    }
                } else {
                    if (failuresCounter.decrementAndGet() == 0) {
                        logger.error("Error in comparing merkle trees", t);
                        try {
                            session.sendError(Response.INTERNAL_ERROR, "Error in comparing merkle trees");
                        } catch (IOException e) {
                            logger.error("Error in sending response", e);
                        }
                    }
                }
            }).isCancelled()) {
                logger.error("Canceled task");
                session.sendError(Response.INTERNAL_ERROR, "Canceled task");
                return;
            }
        }
    }
    
    /**
     * Entity request handler.
     *
     * @param id       request's param
     * @param replicas replica configuration
     * @param request  sent request
     * @param session  current session for network interaction
     * @throws IOException rethrow from {@link HttpSession#sendResponse} and {@link HttpEntityHandler#entity}
     */
    @Path("/v0/entity")
    public void entity(@Param(value = "id", required = true) final String id,
                       @Param(value = "replicas") final String replicas,
                       @NotNull final Request request,
                       @NotNull final HttpSession session) throws IOException {
        if (Utility.invalid(id)) {
            logger.error("Empty key");
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
            throw new IllegalArgumentException("Empty key");
        }
        
        httpSyncEntityEntitiesService.entity(id, replicas, request, session);
    }
    
    /**
     * Entities request handler. Creates stream of records in
     * format(key '\n' value) that contains in range [start; end).
     *
     * @param start   start of range
     * @param end     end of range
     * @param session session for streaming
     * @throws IOException rethrow from {@link HttpSession#sendError} and {@link StreamingSession#stream}
     */
    @Path("/v0/entities")
    public void entities(@Param(value = "start", required = true) final String start,
                         @Param(value = "end") final String end,
                         final HttpSession session) throws IOException {
        if (Utility.invalid(start)) {
            session.sendError(Response.BAD_REQUEST, "Invalid start");
            return;
        }
        
        httpSyncEntityEntitiesService.entities(start, end, ((StreamingSession) session));
    }
    
    @Override
    public synchronized void stop() {
        super.stop();
        try {
            Files.delete(rootDirectory());
            httpSyncEntityEntitiesService.close();
        } catch (IOException e) {
            logger.error("Error in closing entity service", e);
        }
    }
}
