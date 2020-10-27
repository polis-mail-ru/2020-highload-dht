package ru.mail.polis.service.ivlev;

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReplicaService {
    private final int confirm;
    private final int from;

    public ReplicaService(final int confirm, final int from) {
        this.confirm = confirm;
        this.from = from;
    }

    public int getConfirm() {
        return confirm;
    }

    public int getFrom() {
        return from;
    }

    /**
     * Создание ReplicaService.
     *
     * @param replica replica
     * @return new ReplicaService
     */
    @NotNull
    public static ReplicaService of(@NotNull final String replica) {
        final List<String> values = Splitter.on('/').splitToList(replica);
        if (values.size() != 2) {
            throw new IllegalArgumentException();
        }
        return new ReplicaService(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)));
    }
}
