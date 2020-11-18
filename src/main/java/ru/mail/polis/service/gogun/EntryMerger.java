package ru.mail.polis.service.gogun;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class EntryMerger {

    private final int ack;
    @NotNull
    private final Collection<Entry> entries;

    /**
     * This class provides merging all responses from nodes to one, that client will get.
     *
     * @param entries list of responses
     * @param ack     number of responses to say, that answer is correct
     */
    public EntryMerger(@NotNull final Collection<Entry> entries, final int ack) {
        this.entries = entries;
        this.ack = ack;
        this.entries.removeIf((e) -> e.getStatus() == 500);
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
        Entry latestResponse = new Entry();
        latestResponse.setTimestamp(Long.MIN_VALUE);
        for (final Entry entry : entries) {
            final long timestamp = entry.getTimestamp();
            switch (entry.getStatus()) {
                case 404:
                    if (timestamp == Entry.ABSENT) {
                        notFoundResponsesCount++;
                    } else {
                        latestResponse = getLatest(entry, latestResponse, timestamp);
                    }
                    break;
                case 200:
                    latestResponse = getLatest(entry, latestResponse, timestamp);
                    break;
                default:
                    break;
            }
        }

        if (entries.size() == notFoundResponsesCount
                || latestResponse.getStatus() == 404) {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }

        return Response.ok(latestResponse.getBody());
    }

    Response mergePutResponses() {
        if (entries.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.CREATED, Response.EMPTY);
    }

    Response mergeDeleteResponses() {
        if (entries.size() < ack) {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }

        return new Response(Response.ACCEPTED, Response.EMPTY);
    }
}
