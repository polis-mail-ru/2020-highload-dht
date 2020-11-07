package ru.mail.polis.dao.s3ponia;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;

public interface Table extends Closeable {
    int getGeneration();
    
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
