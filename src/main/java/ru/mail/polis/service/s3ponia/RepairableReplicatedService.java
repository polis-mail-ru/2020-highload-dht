package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.s3ponia.DiskTable;
import ru.mail.polis.util.Proxy;
import ru.mail.polis.util.merkletree.MerkleTree;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RepairableReplicatedService extends ReplicatedService implements RepairableService {
    private static final Logger logger = LoggerFactory.getLogger(RepairableReplicatedService.class);
    private static final int RANGES_COUNT = 32_768;
    /**
     * Creates a new {@link ReplicatedService} with given {@link AsyncService} and {@link ShardingPolicy}.
     *
     * @param service    {@link HttpEntityHandler} base service for proxy handle
     * @param daoService {@link DaoService} service for interacting with dao
     * @param policy     {@link ShardingPolicy} replica policy
     */
    public RepairableReplicatedService(@NotNull AsyncService service, DaoService daoService, @NotNull ShardingPolicy<ByteBuffer, String> policy) {
        super(service, daoService, policy);
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
                           .header(Proxy.PROXY_HEADER, policy.homeNode())
                           .uri(new URI(node + "/v0/syncRange?start=" + start + "&end=" + end))
                           .timeout(Duration.ofSeconds(1))
                           .GET()
                           .build();
        } catch (URISyntaxException e) {
            logger.error("Error in uri parsing", e);
            throw new RuntimeException("Error in URI parsing", e);
        }
    }
    
    private CompletableFuture<Path> repair(@NotNull final String node,
                                           @NotNull final MismatchedRanges.Range range,
                                           @NotNull final java.nio.file.Path pathSave) {
        final var request = syncRangeRequest(node, range.start(), range.end());
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(pathSave))
                       .thenApply(HttpResponse::body);
    }
    
    @Override
    public Response repair(final long start, final long end) {
        final var snapshot = daoService.snapshot();
        
        final var tree = snapshot.merkleTree(RANGES_COUNT, start, end);
        final var mismatchedNodes = new MismatchedRanges(tree);
        
        for (final var node : policy.all()) {
            if (node.equals(policy.homeNode())) {
                continue;
            }
            final var request = merkleRequest(node, start, end);
            try {
                final MerkleTree merkleTree;
                try {
                    merkleTree = new MerkleTree(
                            httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).body()
                    );
                } catch (IOException e) {
                    logger.error("Error in receiving merkle tree from {}", node, e);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                }
                final var nodeRangesMismatch = mismatchedNodes.mismatchedNodes(merkleTree);
                for (final var r :
                        nodeRangesMismatch) {
                    java.nio.file.Path pathSave;
                    do {
                        pathSave = daoService.tempFile();
                    } while (Files.exists(pathSave));
                    try {
                        Files.createFile(pathSave);
                    } catch (IOException e) {
                        logger.error("Error in creating file", e);
                        return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                    }
                    try {
                        final var path = repair(node, r, pathSave).get();
                        daoService.merge(DiskTable.of(path));
                    } catch (ExecutionException | RuntimeException | DaoOperationException e) {
                        logger.error("Error in repairing", e);
                        return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Error in receiving merkle tree from {}", node, e);
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
            }
        }
        return Response.ok(Response.EMPTY);
    }
    
    @Override
    public MerkleTree merkleTree(final long start, final long end) {
        return daoService.snapshot().merkleTree(RANGES_COUNT, start, end);
    }
}
