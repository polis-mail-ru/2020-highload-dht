package ru.mail.polis.service.mrsandman5.replication;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.dao.impl.DAOImpl;
import ru.mail.polis.dao.impl.models.Cell;
import ru.mail.polis.utils.ByteUtils;
import ru.mail.polis.utils.ResponseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
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
    private final State state;

    private Entry(final long timestamp,
                 @Nullable final byte[] data,
                 @NotNull final State state) {
        this.timestamp = timestamp;
        this.data = data;
        this.state = state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public byte[] getData() {
        return data;
    }

    @NotNull
    public State getState() {
        return state;
    }

    @Override
    public int compareTo(@NotNull final Entry o) {
        return -Long.compare(timestamp, o.timestamp);
    }

    public static Entry present(final long timestamp,
                                @NotNull final byte[] data) {
        return new Entry(timestamp, data, State.PRESENT);
    }

    public static Entry removed(final long timestamp) {
        return new Entry(timestamp, null, State.REMOVED);
    }

    public static Entry absent() {
        return new Entry(-1, null, State.ABSENT);
    }

    @NotNull
    public static Entry mergeEntries(@NotNull final Collection<Entry> values) {
        return values.stream()
                .filter(value -> value.getState() != State.ABSENT)
                .max(Comparator.comparingLong(Entry::getTimestamp))
                .orElseGet(Entry::absent);
    }

    @NotNull
    public static Entry responseToEntry(@NotNull final Response response) throws IOException {
        final String timestamp = response.getHeader(ResponseUtils.TIMESTAMP);
        final int status = response.getStatus();
        if (status == 200) {
            if (timestamp == null) {
                throw new IllegalArgumentException("Wrong input data");
            }
            return Entry.present(Long.parseLong(timestamp),response.getBody());
        } else if (status == 404) {
            if (timestamp == null) {
                return Entry.absent();
            } else {
                return Entry.removed(Long.parseLong(timestamp));
            }
        } else {
            throw new IOException("Wrong status");
        }
    }

    @NotNull
    public static Response entryToResponse(@NotNull final Entry entry) {
        final Response result;
        switch (entry.getState()) {
            case PRESENT:
                result = ResponseUtils.nonemptyResponse(Response.OK, entry.getData());
                result.addHeader(ResponseUtils.TIMESTAMP + entry.getTimestamp());
                return result;
            case REMOVED:
                result = ResponseUtils.emptyResponse(Response.NOT_FOUND);
                result.addHeader(ResponseUtils.TIMESTAMP + entry.getTimestamp());
                return result;
            case ABSENT:
                return ResponseUtils.emptyResponse(Response.NOT_FOUND);
            default:
                throw new IllegalArgumentException("Wrong input data");
        }
    }

    public static Entry entryFromBytes(@NotNull final ByteBuffer key,
                                       @NotNull final DAOImpl dao) throws IOException {
        final Iterator<Cell> cells = dao.entryIterators(key);
        if (!cells.hasNext()) {
            return Entry.absent();
        }

        final Cell cell = cells.next();
        if (!cell.getKey().equals(key)) {
            return Entry.absent();
        }

        if (cell.getValue().isTombstone()) {
            return Entry.removed(cell.getValue().getTimestamp());
        } else {
            final ByteBuffer value = cell.getValue().getData();
            final byte[] buf = ByteUtils.toByteArray(Objects.requireNonNull(value));
            return Entry.present(cell.getValue().getTimestamp(), buf);
        }
    }
}
