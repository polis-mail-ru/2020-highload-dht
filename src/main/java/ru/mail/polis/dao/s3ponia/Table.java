package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public interface Table extends Closeable {
    public int getGeneration();

    public interface ICell extends Comparable<ICell> {
        @NotNull
        ByteBuffer getKey();

        @NotNull
        Value getValue();
    }

    public static class Cell implements ICell {
        @NotNull
        private final ByteBuffer key;
        @NotNull
        private final Value value;

        protected Cell(@NotNull final ByteBuffer key, @NotNull final Value value) {
            this.key = key;
            this.value = value;
        }

        static Cell of(@NotNull final ByteBuffer key, @NotNull final Value value) {
            return new Cell(key, value);
        }

        @Override
        @NotNull
        public ByteBuffer getKey() {
            return key.asReadOnlyBuffer();
        }

        @Override
        @NotNull
        public Value getValue() {
            return value;
        }

        @Override
        public int compareTo(@NotNull final ICell o) {
            return Comparator.comparing(ICell::getKey).thenComparing(ICell::getValue).compare(this, o);
        }
    }

    public static class Value implements Comparable<Value> {
        private final ByteBuffer byteBuffer;
        private static final long DEAD_FLAG = 0x4000000000000000L;
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

        static Value dead(final int generation) {
            return new Value(ByteBuffer.allocate(0), System.currentTimeMillis(), generation).setDeadFlag();
        }

        static Value of(final ByteBuffer value, final int generation) {
            return new Value(value, System.currentTimeMillis(), generation);
        }

        static Value of(final ByteBuffer value, final long deadFlagTimeStamp, final int generation) {
            return new Value(value, deadFlagTimeStamp, generation);
        }

        ByteBuffer getValue() {
            return byteBuffer.asReadOnlyBuffer();
        }

        Value setDeadFlag() {
            return Value.of(byteBuffer, deadFlagTimeStamp | DEAD_FLAG, generation);
        }

        Value unsetDeadFlag() {
            return Value.of(byteBuffer, deadFlagTimeStamp & ~DEAD_FLAG, generation);
        }

        boolean isDead() {
            return (this.deadFlagTimeStamp & DEAD_FLAG) != 0;
        }

        public long getDeadFlagTimeStamp() {
            return deadFlagTimeStamp + generation;
        }

        public long getTimeStamp() {
            return deadFlagTimeStamp & ~DEAD_FLAG;
        }

        public int getGeneration() {
            return generation;
        }

        @Override
        public int compareTo(@NotNull final Value o) {
            return Comparator.comparing(Value::getGeneration).reversed().compare(this, o);
        }
    }

    public int size();

    public Iterator<ICell> iterator();

    /**
     * Provides iterator (possibly empty) over {@link Cell}s starting at "from" key (inclusive)
     * in <b>ascending</b> order according to {@link Cell#compareTo(ICell)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    public Iterator<ICell> iterator(@NotNull final ByteBuffer from);

    public ByteBuffer get(@NotNull final ByteBuffer key);

    public void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value);

    public void remove(@NotNull final ByteBuffer key);
}
