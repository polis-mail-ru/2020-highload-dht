package ru.mail.polis.service.boriskin;

import one.nio.http.Request;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

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
    @NotNull
    private final RequestMethod method;

    private final boolean alreadyProxied;

    public enum RequestMethod {
        GET, PUT, DELETE
    }

    @NotNull
    public RequestMethod getMethod() {
        return method;
    }

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

        switch (request.getMethod()) {
            case METHOD_GET:
                method = RequestMethod.GET;
                break;
            case METHOD_PUT:
                method = RequestMethod.PUT;
                break;
            case METHOD_DELETE:
                method = RequestMethod.DELETE;
                break;
            default:
                throw new IllegalArgumentException("Неверный метод");
        }
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
