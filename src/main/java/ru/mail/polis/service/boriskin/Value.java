package ru.mail.polis.service.boriskin;

import com.google.common.collect.Iterators;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.dao.Iters;
import ru.mail.polis.dao.boriskin.NewDAO;
import ru.mail.polis.dao.boriskin.TableCell;
import ru.mail.polis.dao.boriskin.TableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

final class Value implements Comparable<Value> {
    private static final String TIMESTAMP_HEADER = "X-OK-Timestamp";

    private static final Value ABSENT =
            new Value(null, -1, State.ABSENT);

    @Nullable
    private final byte[] data;

    private final long timeStamp;

    @NotNull
    private final State state;

    private enum State {
        PRESENT, REMOVED, ABSENT
    }

    private Value(
            @Nullable final byte[] data,
            final long timeStamp,
            @NotNull final State state) {
        this.data =
                data == null
                        ? null : Arrays.copyOf(data, data.length);
        this.timeStamp = timeStamp;
        this.state = state;
    }

    @NotNull
    static Value isPresent(
            @NotNull final byte[] data,
            final long timeStamp) {
        return new Value(data, timeStamp, State.PRESENT);
    }

    @NotNull
    static Value wasRemoved(
            final long timeStamp) {
        return new Value(null, Math.abs(timeStamp), State.REMOVED);
    }

    @NotNull
    static Value isAbsent() {
        return ABSENT;
    }

    @NotNull
    public static Value from(
            @NotNull final HttpResponse<byte[]> response) {
        final HttpHeaders headers = response.headers();
        final String timestamp =
                headers
                        .firstValue(TIMESTAMP_HEADER.toLowerCase(Locale.ENGLISH))
                        .orElse(null);

        switch (response.statusCode()) {
            case 200:
                if (timestamp == null) {
                    throw new IllegalArgumentException("Неверные данные на вход");
                }
                return isPresent(response.body(), Long.parseLong(timestamp));
            case 404:
                if (timestamp == null) {
                    return isAbsent();
                } else {
                    return wasRemoved(Long.parseLong(timestamp));
                }
            default:
                throw new IllegalArgumentException("Неподходящий ответ");
        }
    }

    @NotNull
    public static Value from(
            @NotNull final NewDAO dao,
            @NotNull final ByteBuffer key) throws IOException {
        final Iterator<TableCell> cellIterator = getIteratorOverValuesForReplicas(dao, key);
        if (!cellIterator.hasNext()) {
            return Value.isAbsent();
        }
        final TableCell tableCell = cellIterator.next();
        if (!tableCell.getKey().equals(key)) {
            return Value.isAbsent();
        }
        if (tableCell.getVal().wasRemoved()) {
            return Value.wasRemoved(tableCell.getVal().getTimeStamp());
        } else {
            final ByteBuffer data = tableCell.getVal().getData();
            final byte[] buffer;
            if (!data.hasRemaining()) {
                buffer = Response.EMPTY;
            } else {
                final byte[] bytes = new byte[data.remaining()];
                data.duplicate().get(bytes);
                buffer = bytes;
            }
            return Value.isPresent(buffer, tableCell.getVal().getTimeStamp());
        }
    }

    private static Iterator<TableCell> getIteratorOverValuesForReplicas(
            @NotNull final NewDAO dao,
            @NotNull final ByteBuffer point) throws IOException {
        final TableSet tableSet = dao.getTableSet();
        final List<Iterator<TableCell>> iterators =
                new ArrayList<>(dao.getTableSet().ssTableCollection.size() + 1);
        tableSet.ssTableCollection.descendingMap().values().forEach(val -> {
            try {
                iterators.add(val.iterator(point));
            } catch (IOException ioException) {
                throw new UncheckedIOException(ioException);
            }
        });
        iterators.add(tableSet.currMemTable.iterator(point));
        final Iterator<TableCell> mergedTableCells =
                Iterators.mergeSorted(iterators, Comparator.naturalOrder());
        return Iters.collapseEquals(
                mergedTableCells,
                TableCell::getKey);
    }

    @NotNull
    public static Response transform(
            @NotNull final Value value,
            final boolean alreadyProxied) {
        Response result;
        switch (value.getState()) {
            case PRESENT:
                result =
                        value.getData() == null
                                ? new Response(Response.NOT_FOUND, Response.EMPTY) :
                                new Response(Response.OK, value.getData());
                if (alreadyProxied) {
                    result.addHeader(TIMESTAMP_HEADER + ": " + value.getTimeStamp());
                }
                return result;
            case REMOVED:
                result = new Response(Response.NOT_FOUND, Response.EMPTY);
                if (alreadyProxied) {
                    result.addHeader(TIMESTAMP_HEADER + ": " + value.getTimeStamp());
                }
                return result;
            case ABSENT:
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            default:
                throw new IllegalArgumentException("Неверные данные на вход");
        }
    }

    static Value merge(
            @NotNull final Collection<Value> values) {
        return values.stream()
                .filter(v -> v.getState() != State.ABSENT)
                .max(Value::compareTo)
                .orElseGet(Value::isAbsent);
    }

    @Nullable
    byte[] getData() {
        return data == null
                ? null : Arrays.copyOf(data, data.length);
    }

    long getTimeStamp() {
        return timeStamp;
    }

    @NotNull
    State getState() {
        return state;
    }

    @Override
    public int compareTo(@NotNull final Value value) {
        return Long.compare(
                Math.abs(timeStamp),
                Math.abs(value.timeStamp));
    }
}
