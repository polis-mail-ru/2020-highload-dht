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
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpSyncableService extends HttpBasicService {
    private static final Logger logger = LoggerFactory.getLogger(HttpEntityService.class);
    private static final int RANGES_COUNT = 256;
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
                                  .connectTimeout(Duration.ofSeconds(1))
                                  .executor(executor)
                                  .build();
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
                     @NotNull final HttpSession session) throws IOException {
        final var snapshot = daoService.snapshot();
        final var pathSave = Paths.get(counter.incrementAndGet() + ".back");
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
    }
    
    private HttpRequest merkleRequest(@NotNull final String node) {
        try {
            return HttpRequest.newBuilder()
                           .GET()
                           .uri(new URI(node + "/v0/merkleTree"))
                           .timeout(Duration.ofSeconds(1))
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
                           .GET()
                           .uri(new URI(node + "/v0/syncRange?start=" + start + "&end=" + end))
                           .timeout(Duration.ofSeconds(1))
                           .build();
        } catch (URISyntaxException e) {
            logger.error("Error in uri parsing", e);
            throw new RuntimeException("Error in URI parsing", e);
        }
    }
    
    private static class Range {
        private final int start;
        private final int end;
        
        private Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public static Range fromNode(@NotNull final MerkleTree.Node node) {
            return new Range(
                    node.minValueIndex() * RANGES_COUNT,
                    node.maxValueIndex() * RANGES_COUNT
            );
        }
        
        public int start() {
            return start;
        }
        
        public int end() {
            return end;
        }
    }
    
    private void repair(@NotNull final String node,
                        @NotNull final Range range) throws IOException {
        final var request = syncRangeRequest(node, range.start(), range.end());
        final var pathSave = Paths.get(counter.incrementAndGet() + ".daoRange");
        Files.createFile(pathSave);
        
        if (httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(pathSave))
                    .thenApply(HttpResponse::body)
                    .whenComplete((c, t) -> {
                        if (t == null) {
                            final var diskTable = DiskTable.of(c);
                            final var it = diskTable.iterator();
                            while (it.hasNext()) {
                                final var cell = it.next();
                                try {
                                    if (cell.getValue().isDead()) {
                                        daoService.delete(cell.getKey(),
                                                cell.getValue().getTimeStamp());
                                    } else {
                                        daoService.put(cell.getKey(),
                                                cell.getValue().getValue(),
                                                cell.getValue().getTimeStamp());
                                    }
                                } catch (DaoOperationException e) {
                                    logger.error("Error in dao operation", e);
                                }
                            }
                        } else {
                            logger.error("Error in repairing range = ({}; {})", range.start(), range.end(), t);
                        }
                    }).isCancelled()) {
            logger.error("Canceled task");
        }
    }
    
    @Path("/v0/repair")
    public void repair(@NotNull final HttpSession session) throws IOException {
        final var snapshot = daoService.snapshot();
        
        final var tree = snapshot.merkleTree(RANGES_COUNT);
        final var mismatchedNodes = new MismatchedNodes(tree.root());
        
        for (final var node :
                nodes) {
            final var request = merkleRequest(node);
            final var nodeRangesMismatch =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApply(HttpResponse::body)
                            .thenApply(t -> {
                                final var merkleTree = new MerkleTree(t);
                                final var mismatchedRanges =
                                        mismatchedNodes.mismatchedNodes(merkleTree.root());
                                final var ranges = new ArrayList<Range>();
                                mismatchedRanges.forEach(m -> ranges.add(
                                        Range.fromNode(m.first())
                                        )
                                );
                                return ranges;
                            });
            if (nodeRangesMismatch.whenComplete((a, t) -> {
                if (t == null) {
                    a.forEach(r -> {
                        try {
                            repair(node, r);
                        } catch (IOException exception) {
                            logger.error("Error in repairing", exception);
                        }
                    });
                } else {
                    logger.error("Error in comparing merkle trees", t);
                }
            }).isCancelled()) {
                logger.error("Canceled task");
                session.sendError(Response.INTERNAL_ERROR, "Canceled task");
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
            httpSyncEntityEntitiesService.close();
        } catch (IOException e) {
            logger.error("Error in closing entity service", e);
        }
    }
}
