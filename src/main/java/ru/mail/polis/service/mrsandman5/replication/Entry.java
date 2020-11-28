package ru.mail.polis.service.mrsandman5.replication;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.utils.ByteUtils;
import ru.mail.polis.utils.IteratorUtils;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

public final class Entry implements Comparable<Entry> {

    public enum State {
        PRESENT,
        REMOVED,
        ABSENT
    }

    private final long timestamp;
    @Nullable
    private final byte[] data;
    @NotNull
    private final State state;
    @Nullable
    private final Instant expires;

    private Entry(final long timestamp,
                  @Nullable final byte[] data,
                  @NotNull final State state,
                  @Nullable final Instant expires) {
        this.timestamp = timestamp;
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        this.state = state;
        this.expires = expires;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public byte[] getData() {
        return data == null ? null : data.clone();
    }

    @NotNull
    public State getState() {
        return state;
    }

    @Nullable
    public Instant getExpires() {
        return expires;
    }

    @Override
    public int compareTo(@NotNull final Entry o) {
        return -Long.compare(timestamp, o.timestamp);
    }

    public static Entry present(final long timestamp,
                                @NotNull final byte[] data,
                                @Nullable final Instant expires) {
        return new Entry(timestamp, data, State.PRESENT, expires);
    }

    public static Entry removed(final long timestamp,
                                @Nullable final Instant expires) {
        return new Entry(timestamp, null, State.REMOVED, expires);
    }

    public static Entry absent() {
        return new Entry(-1, null, State.ABSENT, null);
    }

    /** Merge all existed Entries comparing to their timestamp.
     * @param entries - collection of nodes' Entries.
     * @return target Entry.
     * */
    @NotNull
    private static Entry mergeExistEntriesWithTimestamp(@NotNull final Collection<Entry> entries) {
        return entries.stream()
                .filter(value -> value.getState() != State.ABSENT)
                .max(Comparator.comparingLong(Entry::getTimestamp))
                .orElseGet(Entry::absent);
    }

    @NotNull
    public static Response entriesToResponse(@NotNull final Collection<Entry> entries) {
        final Entry entry = Entry.mergeExistEntriesWithTimestamp(entries);
        return entryToResponse(entry);
    }

    /** Convert Entry to service Response.
     * @param entry - source Entry.
     * @return target Response.
     * */
    @NotNull
    public static Response entryToResponse(@NotNull final Entry entry) {
        final Response result;
        switch (entry.getState()) {
            case PRESENT:
                result = ResponseUtils.nonemptyResponse(Response.OK, entry.getData());
                result.addHeader(ResponseUtils.getTimestamp(entry));
                return result;
            case REMOVED:
                result = ResponseUtils.emptyResponse(Response.NOT_FOUND);
                result.addHeader(ResponseUtils.getTimestamp(entry));
                return result;
            case ABSENT:
                return ResponseUtils.emptyResponse(Response.NOT_FOUND);
            default:
                throw new IllegalStateException("Wrong input data");
        }
    }

    /** Get Entry data from ByteBuffer.
     * @param key - source ByteBuffer.
     * @param dao - LSM-database
     * @return target Entry.
     * */
    public static Entry entryFromBytes(@NotNull final ByteBuffer key,
                                       @NotNull final DAOImpl dao) throws IOException {
        final Iterator<Cell> cells = IteratorUtils.entryIterators(key, dao);
        if (!cells.hasNext()) {
            return Entry.absent();
        }
        final Cell cell = cells.next();
        if (!cell.getKey().equals(key)) {
            return Entry.absent();
        }
        if (cell.getValue().isTombstone()
                || (!Instant.MAX.equals(cell.getValue().getExpire())
                && cell.getValue().isExpired())) {
            return Entry.removed(cell.getValue().getTimestamp(), cell.getValue().getExpire());
        } else {
            final ByteBuffer value = cell.getValue().getData();
            final byte[] buf = ByteUtils.toByteArray(Objects.requireNonNull(value));
            return Entry.present(cell.getValue().getTimestamp(),
                    buf,
                    Instant.MAX.equals(cell.getValue().getExpire()) ? null : cell.getValue().getExpire());
        }
    }
}
