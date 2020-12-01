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
    private final Map<String, StreamHttpClient> pool;

    public RangeRequestHelper(final DAO dao, final Topology<String> topology,
                              final Map<String, StreamHttpClient> pool) {
        this.topology = topology;
        this.dao = dao;
        this.pool = pool;
    }

    public void parseRequest(final ByteBuffer start, final ByteBuffer end, final Context context) {
        final StreamingSession streamSession = (StreamingSession) context.getSession();
        try {
            if (context.isProxy()) {
                runOnCurrentNode(start, end, streamSession);
            } else {
                context.getRequest().addHeader(PROXY_HEADER);
                runProxied(start, end, streamSession, context);
            }
        } catch (IOException e) {
            log.error("Error while working range request", e);
            sendLoggedResponse(context.getSession(), new Response(Response.INTERNAL_ERROR, Response.EMPTY));
        }

    }

    public void sendLoggedResponse(final HttpSession session, final Response response) {
        try {
            session.sendResponse(response);
        } catch (IOException e) {
            log.error(SERVER_ERROR, e);
        }
    }

    private void runOnCurrentNode(final ByteBuffer start, final ByteBuffer end,
                                  final StreamingSession streamSession) throws IOException {
        final Iterator<Record> iterator = dao.range(start, end);
        streamSession.stream(iterator);
    }

    void runProxied(final ByteBuffer start, final ByteBuffer end,
                    final StreamingSession streamSession, Context context) throws IOException {
        context.setRangeStart(start);
        context.setRangeEnd(end);
        context.setStreamingSession(streamSession);
        final Iterator<String> nodes = topology.all().iterator();
        final List<Iterator<Record>> iterators = new ArrayList<>();
        try {
            workProxied(context, nodes, iterators);
        } catch (IOException e) {
            log.error("Error while working proxied range request", e);
            throw new IOException("Error in proxied processor", e);
        }
    }

    private void workProxied(final Context context,
                             final Iterator<String> nodes, final List<Iterator<Record>> iterators)
            throws IOException {
        if (!nodes.hasNext()) {
            handleStreamEnd(context, iterators);
            return;
        }
        final String node = nodes.next();
        if (topology.isMe(node)) {
            workOnTheNode(context, nodes, iterators);
        } else {
            final StreamHttpClient streamHttpClient = pool.get(node);
            try {
                streamHttpClient.invokeStream(context.getRequest(), iterator -> {
                    if (iterator.getResponse().getStatus() != 200 || iterator.isNotAvailable()) {
                        log.error("Unexpected response from node {}", node);
                        throw new IOException("Unexpected response from node");
                    }
                    final Iterator<Record> recordIterator = Iterators.transform(iterator, (bytes) -> {
                        assert bytes != null;
                        final int delimiterIdx = Bytes.indexOf(bytes, EOL[0]);
                        final ByteBuffer keyBytes = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, delimiterIdx));
                        final ByteBuffer valueBytes = ByteBuffer.wrap(
                                Arrays.copyOfRange(bytes, delimiterIdx + 1, bytes.length));
                        return Record.of(keyBytes, valueBytes);
                    });
                    iterators.add(recordIterator);
                    workProxied(context, nodes, iterators);
                });
            } catch (InterruptedException | PoolException | IOException | HttpException e) {
                log.error("Unexpected response from node {}", node);
                throw new IOException("Unexpected response from node", e);
            }
        }
    }

    private void workOnTheNode(Context context,
                               Iterator<String> nodes, List<Iterator<Record>> iterators) throws IOException {
        Iterator<Record> iterator;
        try {
            iterator = dao.range(context.getRangeStart(), context.getRangeEnd());
        } catch (IOException e) {
            log.error("Error in getting dao range iterator", e);
            throw new IOException("Error in dao range iterator", e);
        }
        iterators.add(iterator);
        workProxied(context, nodes, iterators);
    }

    private void handleStreamEnd(Context context, List<Iterator<Record>> iterators) throws IOException {
        final Iterator<Record> mergedIterators = Iterators.mergeSorted(iterators, Record::compareTo);
        try {
            context.getStreamingSession().stream(mergedIterators);
        } catch (IOException e) {
            log.error("Error while streaming", e);
            throw new IOException("Error while streaming", e);
        }
    }
}



