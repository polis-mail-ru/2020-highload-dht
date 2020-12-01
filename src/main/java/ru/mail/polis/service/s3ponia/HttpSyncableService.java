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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpSyncableService extends HttpBasicService {
    private static final Logger logger = LoggerFactory.getLogger(HttpEntityService.class);
    private static final int RANGES_COUNT = 32_768;
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
    public void merkleTree(
            @Param(value = "start", required = true) final String startParam,
            @Param(value = "end", required = true) final String endParam,
            @NotNull final HttpSession session) throws IOException {
        final long start;
        final long end;
        try {
            start = Long.parseLong(startParam);
            end = Long.parseLong(endParam);
        } catch (NumberFormatException e) {
            logger.error("Error in parsing start({}) and end({})", startParam, endParam, e);
            session.sendError(Response.BAD_REQUEST, "Error in parsing start and end");
            return;
        }
        final MerkleTree tree = daoService.snapshot().merkleTree(RANGES_COUNT, start, end);
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
    
    private HttpRequest merkleRequest(@NotNull final String node,
                                      final long start,
                                      final long end) {
        try {
            return HttpRequest.newBuilder()
                           .uri(new URI(node + "/v0/merkleTree?start=" + start + "&end=" + end))
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
    public void repair(@Param(value = "start") final String startParam,
                       @Param(value = "end") final String endParam,
                       @NotNull final HttpSession session) throws IOException {
        final long start;
        final long end;
    
        try {
            if (startParam != null) {
                start = Long.parseLong(startParam);
            } else {
                start = 0;
            }
            if (endParam != null) {
                end = Long.parseLong(endParam);
            } else {
                end = Long.MAX_VALUE;
            }
        } catch (NumberFormatException e) {
            logger.error("Error in parsing start({}) and end({})", startParam, endParam, e);
            session.sendError(Response.BAD_REQUEST, "Error in parsing start and end");
            return;
        }
        
        final var snapshot = daoService.snapshot();
    
        final var tree = snapshot.merkleTree(RANGES_COUNT, start, end);
        final var mismatchedNodes = new MismatchedRanges(tree);
        
        for (final var node :
                nodes) {
            if (node.equals("http://localhost:" + port)) {
                continue;
            }
            final var request = merkleRequest(node, start, end);
            try {
                final var merkleTree = new MerkleTree(
                        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).body()
                );
                final var nodeRangesMismatch = mismatchedNodes.mismatchedNodes(merkleTree);
                for (final var r :
                        nodeRangesMismatch) {
                    java.nio.file.Path pathSave;
                    do {
                        pathSave = relativePath(counter.incrementAndGet() + "_" + r.start() + "_" + r.end() +
                                                        ".daoRange");
                    } while (Files.exists(pathSave));
                    try {
                        Files.createFile(pathSave);
                    } catch (IOException e) {
                        logger.error("Error in creating file", e);
                        session.sendError(Response.INTERNAL_ERROR, "Error in creating file");
                        return;
                    }
                    try {
                        final var path = repair(node, r, pathSave).get();
                        daoService.merge(DiskTable.of(path));
                    } catch (ExecutionException | RuntimeException | DaoOperationException e) {
                        logger.error("Error in repairing", e);
                        session.sendError(Response.INTERNAL_ERROR, "Error in repairing");
                        return;
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Error in receiving merkle tree from {}", node, e);
                session.sendError(Response.INTERNAL_ERROR, "Error in receiving merkle tree");
                return;
            }
        }
        session.sendResponse(Response.ok(Response.EMPTY));
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
