package ru.mail.polis.util;

import com.google.common.base.Splitter;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.dariagap.Timestamp;

import java.nio.ByteBuffer;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class Replicas {
    private final int ask;
    private final int from;
    private int answers;
    private byte[] freshData = Response.EMPTY;
    private long freshTimestamp;
    private Timestamp.State freshState = Timestamp.State.UNKNOWN;

    private static final Logger log = LoggerFactory.getLogger(Replicas.class);

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_NOT_FOUND = 404;

    /**
     * Config "ask" and "from" from query-parameter "replicas".
     *
     * @param replicas in format ask/from
     */
    public Replicas(@NotNull final String replicas) {
        final List<String> r = Splitter.on('/').splitToList(replicas);
        this.ask = Integer.valueOf(r.get(0));
        this.from = Integer.valueOf(r.get(1));
    }

    /**
     * Config "ask" and "from" from cluster size.
     *
     * @param from number of cluster nodes
     */
    public Replicas(@NotNull final int from) {
        this.ask = from / 2 + 1;
        this.from = from;
    }

    public int getFrom() {
        return from;
    }

    public int getAsk() {
        return ask;
    }

    /**
     * Analyse response from node, count number of answers.
     *
     * @param response - response from node
     * @param method - request method
     */
    public void analyseResponse(final Response response, final int method) {
        if (response.getStatus() == STATUS_OK
                || response.getStatus() == STATUS_CREATED
                || response.getStatus() == STATUS_ACCEPTED) {
            answers++;
            if (method == METHOD_GET) {
                rememberFreshData(response.getBody());
            }
        }
        if (response.getStatus() == STATUS_NOT_FOUND) {
            answers++;
        }
    }

    private void rememberFreshData(final byte[] data) {
        final Timestamp timestamp;
        timestamp = Timestamp.getTimestampByData(ByteBuffer.wrap(data));
        if (timestamp.getTimestampValue() > freshTimestamp) {
            freshTimestamp = timestamp.getTimestampValue();
            freshData = timestamp.getData();
            freshState = timestamp.getState();
        }
    }

    private String formCorrectAnswer(final int method) {
        switch (method) {
            case METHOD_PUT:
                return Response.CREATED;
            case METHOD_DELETE:
                return Response.ACCEPTED;
            default:
                log.error("Unknown method");
                return Response.METHOD_NOT_ALLOWED;
        }
    }

    private Response formGetResponse() {
        if (freshState == Timestamp.State.DELETED
                || freshState == Timestamp.State.UNKNOWN) {
            log.error("No data founded in replicas to sent to client");
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        return new Response(Response.OK,freshData);
    }

    /**
     * Form final response to client.
     *
     * @param method - request method
     */
    public Response formFinalResponse(final int method) {
        if (answers >= ask) {
            if (method == METHOD_GET) {
                return formGetResponse();
            } else {
                return new Response(formCorrectAnswer(method), Response.EMPTY);
            }
        } else {
            log.error("Not enough replicas returned answers");
            return new Response("504", "Not Enough Replicas".getBytes(UTF_8));
        }
    }

    /**
     * Clean fields "answers", "freshData", "freshTimestamp", "freshState" to initial values.
     */
    public void clean() {
        answers = 0;
        freshData = Response.EMPTY;
        freshTimestamp = 0L;
        freshState = Timestamp.State.UNKNOWN;
    }
}
