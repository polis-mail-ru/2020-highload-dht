package ru.mail.polis.dao.boriskin;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class Value {

    // либо ByteBuffer, либо могилка + Time Stamp (версия)
    private final long timeStamp;
    private final ByteBuffer data;

    public Value(
            final long timeStamp,
            @NotNull final ByteBuffer data) {
        this.timeStamp = timeStamp;
        this.data = data;
    }

    public Value(
            final long timeStamp) {
        this.timeStamp = timeStamp;
        this.data = null;
    }

    /**
     * Метод доступа к данным в ячейке Value: значение+timestamp.
     *
     * @param data данные
     * @return Возвращает из ячейки значения версию и значение
     */
    public static Value get(
            final ByteBuffer data) {
        // по рекомендации из лекции в качестве значения версии
        // используется отметка времени
        return new Value(
                Utils.getTime(), data.duplicate());
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Метод создания нового ByteBuffer с теми же данными.
     *
     * @return Возвращает новый read-only ByteBuffer с той же data, что и в текущем
     */
    @NotNull
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        return data.asReadOnlyBuffer();
    }

    public boolean wasRemoved() {
        return data == null;
    }
}
