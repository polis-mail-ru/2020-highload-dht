package ru.mail.polis.service.boriskin;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class MetaInfoRequest {

    private static final String PARAM_ID = "id";

    @NotNull
    private final Request request;
    @NotNull
    private final ReplicaFactor replicaFactor;
    @NotNull
    private final String id;
    @NotNull
    private final ByteBuffer value;

    private final boolean alreadyProxied;

    /**
     * Конструктор {@link MetaInfoRequest}.
     *
     * @param request запрос
     * @param replicaFactor фактор репликации
     * @param alreadyProxied истина, когда запрос проксирован
     */
    MetaInfoRequest(
            @NotNull final Request request,
            @NotNull final ReplicaFactor replicaFactor,
            final boolean alreadyProxied) {
        this.request = request;
        this.replicaFactor = replicaFactor;
        this.id = request.getParameter(PARAM_ID).substring(1);
        this.value =
                request.getBody() == null
                        ? ByteBuffer.allocate(0) : ByteBuffer.wrap(request.getBody());
        this.alreadyProxied = alreadyProxied;
    }

    @NotNull
    Request getRequest() {
        return request;
    }

    @NotNull
    ReplicaFactor getReplicaFactor() {
        return replicaFactor;
    }

    @NotNull
    String getId() {
        return id;
    }

    @NotNull
    public ByteBuffer getValue() {
        return value;
    }

    boolean isAlreadyProxied() {
        return alreadyProxied;
    }
}
