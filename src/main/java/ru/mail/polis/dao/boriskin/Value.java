package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class Value implements Comparable<Value> {

    // либо ByteBuffer, либо могилка + Time Stamp (версия)
    private final long timeStamp;
    private final ByteBuffer data;

    @Override
    public int compareTo(@NotNull final Value val) {
        return -Long.compare(timeStamp, val.timeStamp);
    }

    Value(final long timeStamp, final ByteBuffer data) {
        this.timeStamp = timeStamp;
        this.data = data;
    }

    /**
     * Метод доступа к данным в ячейке Value: значение+timestamp.
     *
     * @param data данные
     * @return Возвращает из ячейки значения версию и значение
     */
    public static Value valueOf(final ByteBuffer data) {
        // по рекомендации из лекции в качестве значения версии
        // используется отметка времени
        return new Value(Utils.getTime(), data.duplicate());
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Метод создания нового ByteBuffer с теми же данными.
     *
     * @return Возвращает новый read-only ByteBuffer с той же data, что и в текущем
     */
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        return data.asReadOnlyBuffer();
    }

    static Value tombstone() {
        // у могилки есть версия - тот же Time Stamp
        return new Value(Utils.getTime(), null);
    }

    boolean wasRemoved() {
        return data == null;
    }
}
