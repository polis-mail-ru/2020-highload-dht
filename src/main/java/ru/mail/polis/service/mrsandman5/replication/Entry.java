package ru.mail.polis.service.mrsandman5.replication;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;

public final class Entry implements Comparable<Entry> {

    enum State{
        PRESENT,
        REMOVED,
        ABSENT
    }

    private final long timestamp;
    private final byte[] data;
    private final State state;

    private Entry(final long timestamp,
                 final byte[] data,
                 @NotNull final State state) {
        this.timestamp = timestamp;
        this.data = data;
        this.state = state;
    }

    public long getTimestamp() {
        return timestamp;
    }

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
                                @NotNull final byte[] data){
        return new Entry(timestamp, data, State.PRESENT);
    }

    public static Entry removed(final long timestamp){
        return new Entry(timestamp, null, State.REMOVED);
    }

    public static Entry absent(){
        return new Entry(-1, null, State.ABSENT);
    }

    @NotNull
    public static Entry merge(@NotNull final Collection<Entry> values) {
        return values.stream()
                .filter(value -> value.getState() != State.ABSENT)
                .max(Comparator.comparingLong(Entry::getTimestamp))
                .orElseGet(Entry::absent);
    }
}
