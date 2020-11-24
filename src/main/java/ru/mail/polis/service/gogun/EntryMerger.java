package ru.mail.polis.service.gogun;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class EntryMerger<T> {

    private final int ack;
    @NotNull
    private final Collection<T> entries;

    /**
     * This class provides merging all responses from nodes to one, that client will get.
     *
     * @param entries list of responses
     * @param ack     number of responses to say, that answer is correct
     */
    public EntryMerger(@NotNull final Collection<T> entries, final int ack) {
        this.entries = entries;
        this.ack = ack;
    }

    @NotNull
    private Entry getLatest(final Entry entry, final Entry latestResponse, final long timestamp) {
        final long latestTimestamp = latestResponse.getTimestamp();
        Entry latestCopy = latestResponse;
        if (timestamp > latestTimestamp) {
            latestCopy = entry;
        }

        return latestCopy;
    }

    Response mergeGetResponses() {
        if (entries.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        int notFoundResponsesCount = 0;
        Entry latestResponse = new Entry(Entry.ABSENT, Entry.EMPTY_DATA, Status.ABSENT);
        latestResponse.setTimestamp(Long.MIN_VALUE);
        for (final T entry : entries) {
            final long timestamp = ((Entry) entry).getTimestamp();
            if (timestamp == Entry.ABSENT) {
                notFoundResponsesCount++;
            } else {
                latestResponse = getLatest(((Entry) entry), latestResponse, timestamp);
            }
        }

        if (entries.size() == notFoundResponsesCount
                || latestResponse.getStatus() == Status.REMOVED) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(latestResponse.getBody());
    }

    Response mergePutDeleteResponses(@NotNull final Request request) {
        if (entries.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
        switch (request.getMethod()) {
            case Request.METHOD_PUT:
                return new Response(Response.CREATED, Response.EMPTY);
            case Request.METHOD_DELETE:
                return new Response(Response.ACCEPTED, Response.EMPTY);
            default:
                return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

    }
}
