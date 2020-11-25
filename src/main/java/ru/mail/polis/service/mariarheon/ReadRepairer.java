package ru.mail.polis.service.mariarheon;

import one.nio.http.Request;
import one.nio.util.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class ReadRepairer {
    private static final Logger logger = LoggerFactory.getLogger(AsyncServiceImpl.class);

    private final ReadRepairInfo repairInfo;
    private final RendezvousSharding sharding;
    private final DAO dao;

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
        final var uri = "/v0/entity?" + Util.MYSELF_PARAMETER + "="
                + "&id=" + URLEncoder.encode(record.getKey())
                + "&timestamp=" + record.getTimestamp().getTime();

        int method;
        if (record.isRemoved()) {
            method = Request.METHOD_DELETE;
        } else {
            method = Request.METHOD_PUT;
        }
        final var request = new Request(method, uri, true);
        try {
            request.addHeader("Host: " + new URI(uri).getHost());
        } catch (URISyntaxException e) {
            logger.error("Failed to parse uri", e);
            return;
        }
        request.addHeader("Connection: Keep-Alive");
        if (record.isRemoved()) {
            request.addHeader("Content-Length: 0");
            request.setBody(new byte[]{});
        } else {
            final var val = record.getValue();
            request.addHeader("Content-Length: " + val.length);
            request.setBody(val);
        }
        try {
            final var res = sharding.passOn(node, request)
                .get();
            logger.info("\n" + sharding.getMe() + ": Repair response status = "
                    + res.getStatus() + " from " + node);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to read-repair", e);
        }
    }

    /**
     * Repair nodes with old value.
     */
    public void repair() {
        if (repairInfo == null) {
            return;
        }
        logger.info("\n#start repairing");
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
    }
}
