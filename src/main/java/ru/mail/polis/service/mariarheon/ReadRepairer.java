package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.util.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Class for repairing nodes.
 */
public class ReadRepairer {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    private final ReadRepairInfo repairInfo;
    private final RendezvousSharding sharding;
    private final DAO dao;
    private List<CompletableFuture<Void>> responses;

    /**
     * Create repairer for read-repair functionality.
     *
     * @param sharding - information about other nodes.
     * @param dao - dao implementation.
     * @param repairInfo - all information, required for repairing nodes.
     */
    public ReadRepairer(final RendezvousSharding sharding,
                        final DAO dao,
                        final ReadRepairInfo repairInfo) {
        this.repairInfo = repairInfo;
        this.sharding = sharding;
        this.dao = dao;
    }

    private void passOn(final String node, final Record record) {
        final var uri = URI.create(node + "/v0/entity?id="
                + URLEncoder.encode(record.getKey()));
        int method;
        if (record.isRemoved()) {
            method = Request.METHOD_DELETE;
        } else {
            method = Request.METHOD_PUT;
        }
        byte[] body = null;
        if (!record.isRemoved()) {
            body = record.getValue();
        }
        final var request = RequestFactory.create(uri, method, body,
                record.getTimestamp().getTime());
        final var res = sharding.passOn(request)
                .exceptionally(ex -> {
                    logger.error("Read repair failed for " + node, ex);
                    return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
                })
                .thenAccept(resp -> {
                    logger.info("\n" + sharding.getMe() + ": Repair response status = "
                            + resp.getStatus() + " from " + node);
                })
                .exceptionally(ex -> {
                    logger.error("Read repair: logging failed", ex);
                    return null;
                });
        responses.add(res);
    }

    /**
     * Repair nodes with old value.
     */
    public void repair() {
        logger.info("\n#start repairing");
        responses = new ArrayList<CompletableFuture<Void>>();
        final var rightRecord = repairInfo.getRightRecord();
        final var nodes = repairInfo.getNodes();
        for (final var node : nodes) {
            if (sharding.isMe(node)) {
                logger.info("\n#repair myself (" + node + "):"
                        + Util.loggingValue(rightRecord.getValue()) + " timestamp=("
                        + rightRecord.getTimestamp().getTime() + ")");
                try {
                    rightRecord.save(dao);
                } catch (IOException e) {
                    logger.warn("Failed to read repair local record");
                }
            } else {
                logger.info("\n#repair node " + node + ":"
                        + Util.loggingValue(rightRecord.getValue()) + " timestamp=("
                        + rightRecord.getTimestamp().getTime() + ")");
                passOn(node, rightRecord);
            }
        }
        for (final var resp : responses) {
            try {
                resp.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Read-repair: failed to wait the task \"repair the node\"", e);
            }
        }
    }
}
