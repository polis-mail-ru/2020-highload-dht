package ru.mail.polis.service.boriskin;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReplicaFactor {
    private final int ack;
    private final int from;

    private ReplicaFactor(
            final int ack,
            final int from) {
        this.ack = ack;
        this.from = from;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }

    private static int quorum(final int nodeSetSize) {
        return nodeSetSize / 2 + 1;
    }

    /**
     * Парсинг фактора репликации из формата ack/from.
     *
     * @param replicas строка вида - ack/from
     * @return фактор репликации
     */
    public static ReplicaFactor from(
            @NotNull final String replicas) {
        final List<String> splitted = Splitter.on('/').splitToList(replicas);
        if (splitted.size() != 2) {
            throw new IllegalArgumentException("Неверный фактор репликации: " + replicas);
        }

        final int ack = Integer.parseInt(splitted.get(0));
        final int from = Integer.parseInt(splitted.get(1));

        if (ack < 1 || from < ack) {
            throw new IllegalArgumentException("Неверный RF: " + replicas);
        }

        return new ReplicaFactor(ack,from);
    }

    /**
     * Фактор репликации из количества нод.
     *
     * @param nodeSetSize количество нод
     * @return фактор репликации
     */
    public static ReplicaFactor from(
            final int nodeSetSize) {
        return new ReplicaFactor(
                ReplicaFactor.quorum(nodeSetSize), nodeSetSize);
    }
}
