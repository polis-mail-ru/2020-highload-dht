package ru.mail.polis.service.kate.moreva;

import com.google.common.collect.Iterators;
import com.google.common.primitives.Bytes;
import one.nio.http.HttpException;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Record;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RangeRequestHelper {
    static final String PROXY_HEADER = "X-Proxy:";
    private static final String SERVER_ERROR = "Server error can't send response";
    private static final byte[] EOL = "\n".getBytes(StandardCharsets.UTF_8);

    private static final Logger log = LoggerFactory.getLogger(RangeRequestHelper.class);
    private final DAO dao;
    private final Topology<String> topology;
    private final Map<String, StreamingHttpClient> pool;

    /**
     * RangeRequestHelper constructor.
     * @param dao - dao.
     * @param topology - cluster topology.
     * @param pool - pool of StreamingHttpClients.
     * */
    public RangeRequestHelper(final DAO dao, final Topology<String> topology,
                              final Map<String, StreamingHttpClient> pool) {
        this.topology = topology;
        this.dao = dao;
        this.pool = pool;
    }

    /**
     * Method defines how range request should be handled.
     * */
    public void parseRequest(final ByteBuffer start, final ByteBuffer end, final Context context) {
        final StreamingSession streamSession = (StreamingSession) context.getSession();
        try {
            if (context.isProxy()) {
                context.setRangeStart(start);
                context.setRangeEnd(end);
                context.setStreamingSession(streamSession);
                runOnCurrentNode(context);
            } else {
                context.getRequest().addHeader(PROXY_HEADER + true);
                context.setRangeStart(start);
                context.setRangeEnd(end);
                context.setStreamingSession(streamSession);
                runProxied(context);
            }
        } catch (IOException e) {
            log.error("Error while working range request", e);
            sendLoggedResponse(context.getSession(), new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }

    }

    private void sendLoggedResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }

    private void runOnCurrentNode(final Context context) throws IOException {
        final Iterator<Record> iterator = dao.range(context.getRangeStart(), context.getRangeEnd());
        context.getStreamingSession().setRecordIterator(iterator);
    }

    void runProxied(final Context context) throws IOException {
        final List<String> nodes = topology.all();
        final List<Iterator<Record>> iterators = new ArrayList<>();
        try {
            workProxied(context, nodes, iterators);
        } catch (IOException e) {
            log.error("Error while working proxied range request", e);
            throw new IOException("Error in proxied processor", e);
        }
    }

    private void workProxied(final Context context,
                             final List<String> nodes, final List<Iterator<Record>> iterators) throws IOException {
        for (String node: nodes) {
            if (topology.isMe(node)) {
                iterators.add(workOnTheNode(context));
            } else {
                final StreamingHttpClient streamingHttpClient = pool.get(node);
                try {
                    streamingHttpClient.invokeStream(context.getRequest(), iterator -> {
                        if (iterator.getResponse().getStatus() != 200 || iterator.isNotAvailable()) {
                            log.error("Unexpected response from node {}", node);
                            throw new IOException("Unexpected response from node");
                        }
                        final Iterator<Record> recordIterator = Iterators.transform(iterator, (bytes) -> {
                            assert bytes != null;
                            final int delimiterIdx = Bytes.indexOf(bytes, EOL[0]);
                            final ByteBuffer keyBytes = ByteBuffer.wrap(Arrays.copyOfRange(bytes,
                                    0,
                                    delimiterIdx));
                            final ByteBuffer valueBytes = ByteBuffer.wrap(
                                    Arrays.copyOfRange(bytes, delimiterIdx + 1, bytes.length));
                            return Record.of(keyBytes, valueBytes);
                        });
                        iterators.add(recordIterator);
                    });
                } catch (InterruptedException | PoolException | IOException | HttpException e) {
                    log.error("Unexpected response from node {}", node);
                    throw new IOException("Unexpected response from node", e);
                }
            }
        }
        handleStreamEnd(context, iterators);
    }

    private Iterator<Record> workOnTheNode(final Context context) throws IOException {
        try {
            return dao.range(context.getRangeStart(), context.getRangeEnd());
        } catch (IOException e) {
            log.error("Error in getting dao range iterator", e);
            throw new IOException("Error in dao range iterator", e);
        }
    }

    private void handleStreamEnd(final Context context, final List<Iterator<Record>> iterators) throws IOException {
        final Iterator<Record> mergedIterators = Iterators.mergeSorted(iterators, Record::compareTo);
        try {
            context.getStreamingSession().setRecordIterator(mergedIterators);
        } catch (IOException e) {
            log.error("Error while streaming", e);
            throw new IOException("Error while streaming", e);
        }
    }
}



