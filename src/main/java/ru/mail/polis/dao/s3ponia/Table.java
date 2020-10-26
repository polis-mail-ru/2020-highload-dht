package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;

public interface Table extends Closeable {
    int getGeneration();
    
    interface ICell extends Comparable<ICell> {
        @NotNull
        ByteBuffer getKey();
        
        @NotNull
        Value getValue();
    }
    
    class Cell implements ICell {
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
    
    class Value implements Comparable<Value> {
        private final ByteBuffer byteBuffer;
        private static final long DEAD_FLAG = 1L << 63;
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
    }
    
    int size();
    
    Iterator<ICell> iterator();
    
    /**
     * Provides iterator (possibly empty) over {@link Cell}s starting at "from" key (inclusive)
     * in <b>ascending</b> order according to {@link Cell#compareTo(ICell)}.
     * N.B. The iterator should be obtained as fast as possible, e.g.
     * one should not "seek" to start point ("from" element) in linear time ;)
     */
    Iterator<ICell> iterator(@NotNull final ByteBuffer from);
    
    ByteBuffer get(@NotNull final ByteBuffer key);
    
    void upsert(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value);
    
    void upsertWithTimeStamp(@NotNull final ByteBuffer key, @NotNull final ByteBuffer value,
                             final long timeStamp);
    
    void remove(@NotNull final ByteBuffer key);
    
    void removeWithTimeStamp(@NotNull final ByteBuffer key,
                             final long timeStamp);
}
