package ru.mail.polis.dao.s3ponia;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.service.s3ponia.Header;
import ru.mail.polis.util.Utility;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class Value implements Comparable<Value> {
    public static final String DEADFLAG_TIMESTAMP_HEADER = Utility.DEADFLAG_TIMESTAMP_HEADER;
    public static final Value ABSENT = Value.dead(-1, 0);
    private final ByteBuffer byteBuffer;
    public static final long DEAD_FLAG = 1L << 63;
    private final long deadFlagTimeStamp;
    private final int generation;

    /**
     * Value constructor.
     *
     * @param value             - byte buffer value
     * @param deadFlagTimeStamp - timestamp+dead flag
     * @param generation        - table generation
     */
    public Value(final ByteBuffer value, final long deadFlagTimeStamp, final int generation) {
        this.byteBuffer = value;
        this.deadFlagTimeStamp = deadFlagTimeStamp;
        this.generation = generation;
    }

    public static Value dead(final int generation) {
        return new Value(ByteBuffer.allocate(0), System.currentTimeMillis(), generation).setDeadFlag();
    }

    public static Value dead(final int generation, final long timeStamp) {
        return new Value(ByteBuffer.allocate(0), timeStamp, generation).setDeadFlag();
    }

    public static Value of(final ByteBuffer value, final int generation) {
        return new Value(value, System.currentTimeMillis(), generation);
    }

    public static Value of(final ByteBuffer value, final long deadFlagTimeStamp, final int generation) {
        return new Value(value, deadFlagTimeStamp, generation);
    }

    public ByteBuffer getValue() {
        return byteBuffer.asReadOnlyBuffer();
    }

    public Value setDeadFlag() {
        return Value.of(byteBuffer, deadFlagTimeStamp | DEAD_FLAG, generation);
    }

    public Value unsetDeadFlag() {
        return Value.of(byteBuffer, deadFlagTimeStamp & ~DEAD_FLAG, generation);
    }

    public static boolean isDead(final long deadFlagTimeStamp) {
        return (deadFlagTimeStamp & DEAD_FLAG) != 0;
    }

    public boolean isDead() {
        return (this.deadFlagTimeStamp & DEAD_FLAG) != 0;
    }

    public long getDeadFlagTimeStamp() {
        return deadFlagTimeStamp;
    }

    public static long getTimeStampFromLong(final long deadFlagTimeStamp) {
        return deadFlagTimeStamp & ~DEAD_FLAG;
    }

    public long getTimeStamp() {
        return deadFlagTimeStamp & ~DEAD_FLAG;
    }

    public int getGeneration() {
        return generation;
    }

    public int size() {
        return byteBuffer.capacity();
    }

    @Override
    public int compareTo(@NotNull final Value o) {
        return Comparator.comparing(Value::getTimeStamp)
                .thenComparing(Value::getGeneration)
                .reversed()
                .compare(this, o);
    }

    private static Long getDeadFlagTimeStampFromResponse(@NotNull final Response response) {
        final var header = Header.getHeader(DEADFLAG_TIMESTAMP_HEADER, response);
        if (header == null) {
            throw new IllegalArgumentException("No deadflag timestamp header");
        }
        return Long.parseLong(header.value);
    }

    /**
     * Parse response to Value.
     * @param response parsed response
     * @return Value
     */
    public static Value fromResponse(final Response response) {
        final var timeStamp = getDeadFlagTimeStampFromResponse(response);
        return Value.of(ByteBuffer.wrap(response.getBody()),
                timeStamp, -1);
    }

    /**
     * Response's value validation.
     *
     * @return comparator
     */
    @NotNull
    public static Comparator<Value> valueResponseComparator() {
        return Comparator.comparing(Value::getTimeStamp)
                .reversed()
                .thenComparing((a, b) -> {
                    if (a.isDead()) {
                        return -1;
                    }
                    if (b.isDead()) {
                        return 1;
                    }

                    return 0;
                });
    }
}
