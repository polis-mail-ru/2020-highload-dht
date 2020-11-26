package ru.mail.polis.service.boriskin;

import com.google.common.base.Charsets;
import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.boriskin.NewDAO;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import static ru.mail.polis.service.boriskin.FuturesWorker.getResponses;
import static ru.mail.polis.service.boriskin.NewService.resp;

final class ReplicaWorker {
    private static final Logger logger = LoggerFactory.getLogger(ReplicaWorker.class);

    static final String PROXY_HEADER = "X-OK-Proxy: true";
    static final String PROXY_HEADER_NAME = "X-OK-Proxy";
    static final String PROXY_HEADER_VALUE = "true";

    @NotNull
    private final NewDAO dao;
    @NotNull
    private final Topology<String> topology;
    @NotNull
    private final HttpClient javaNetHttpClient;
    @NotNull
    private final ExecutorService executor;

    ReplicaWorker(
            @NotNull final ExecutorService proxyWorkers,
            @NotNull final ExecutorService executor,
            @NotNull final DAO dao,
            @NotNull final Topology<String> topology) {
        this.dao = (NewDAO) dao;
        this.topology = topology;
        this.javaNetHttpClient =
                HttpClient.newBuilder()
                        .executor(proxyWorkers)
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
        this.executor = executor;
    }

    void getting(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        if (mir.isAlreadyProxied()) {
            doGet(httpSession, mir);
            return;
        }
        final List<String> replicas =
                topology.replicas(
                        ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)),
                        mir.getReplicaFactor().getFrom());
        final ArrayList<Value> values = new ArrayList<>();
        runAsyncIfReplicasContainNode(replicas, () -> {
            try {
                values.add(
                        Value.from(dao, ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8))));
            } catch (IOException ioException) {
                logger.error("Нода: {}. Ошибка в GET {} ",
                        topology.recogniseMyself(), mir.getId(), ioException);
            }
        }).thenCompose(handled -> getResponses(replicas, mir, topology, javaNetHttpClient)
        ).whenComplete((responses, error) -> {
            final Predicate<HttpResponse<byte[]>> success = r -> values.add(Value.from(r));
            final int acks = getNumberOfSuccessfulResponses(
                    getStartAcks(replicas), responses, success);
            final Response response = Value.transform(Value.merge(values), false);
            sendResponseIfExpectedAcksReached(
                    acks, mir.getReplicaFactor().getAck(), response, httpSession);
        }).exceptionally(exception -> {
            logger.error("Ошибка при использовании Future в GET: ", exception);
            return null;
        });
    }

    private int getStartAcks(
            final List<String> replicas) {
        return replicas.contains(topology.recogniseMyself()) ? 1 : 0;
    }

    private void doGet(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        CompletableFuture.runAsync(() -> {
            try {
                final Response response = Value.transform(
                        Value.from(dao, ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8))),
                        true);
                resp(httpSession, response);
            } catch (IOException ioException) {
                resp(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        }, executor).exceptionally(exception -> {
            logger.error("Ошибка при выполнении операции GET (DAO): ", exception);
            return null;
        });
    }

    void upserting(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        if (mir.isAlreadyProxied()) {
            doUpsert(httpSession, mir);
            return;
        }
        final List<String> replicas =
                topology.replicas(
                        ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)),
                        mir.getReplicaFactor().getFrom());
        runAsyncIfReplicasContainNode(replicas, () -> {
            try {
                dao.upsert(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)), mir.getValue());
            } catch (IOException ioException) {
                logger.error("Нода: {}. Ошибка в PUT {}, {} ",
                        topology.recogniseMyself(), mir.getId(), mir.getValue(), ioException);
            }
        }).thenCompose(handled -> getResponses(replicas, mir, topology, javaNetHttpClient)
        ).whenComplete((responses, error) -> getSuccessAndSendIfReachedExpected(
                httpSession, mir, replicas, responses, 201)
        ).exceptionally(exception -> {
            logger.error("Ошибка при использовании Future в UPSERT: ", exception);
            return null;
        });
    }

    private void doUpsert(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        CompletableFuture.runAsync(() -> {
            try {
                dao.upsert(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)), mir.getValue());
                resp(httpSession, new Response(Response.CREATED, Response.EMPTY));
            } catch (NoSuchElementException noSuchElementException) {
                resp(httpSession, new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException ioException) {
                resp(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        }, executor).exceptionally(exception -> {
            logger.error("Ошибка при выполнении операции UPSERT (DAO): ", exception);
            return null;
        });
    }

    void removing(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        if (mir.isAlreadyProxied()) {
            doRemove(httpSession, mir);
            return;
        }
        final List<String> replicas =
                topology.replicas(
                        ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)),
                        mir.getReplicaFactor().getFrom());
        runAsyncIfReplicasContainNode(replicas, () -> {
            try {
                dao.remove(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)));
            } catch (IOException ioException) {
                logger.error("Нода: {}. Ошибка в DELETE {}, {} ",
                        topology.recogniseMyself(), mir.getId(), ioException);
            }
        }).thenCompose(handled -> getResponses(replicas, mir, topology, javaNetHttpClient)
        ).whenComplete((responses, error) -> getSuccessAndSendIfReachedExpected(
                httpSession, mir, replicas, responses, 202)
        ).exceptionally(exception -> {
            logger.error("Ошибка при использовании Future в DELETE: ", exception);
            return null;
        });
    }

    private void doRemove(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir) {
        CompletableFuture.runAsync(() -> {
            try {
                dao.remove(ByteBuffer.wrap(mir.getId().getBytes(Charsets.UTF_8)));
                resp(httpSession, new Response(Response.ACCEPTED, Response.EMPTY));
            } catch (NoSuchElementException noSuchElementException) {
                resp(httpSession, new Response(Response.NOT_FOUND, Response.EMPTY));
            } catch (IOException ioException) {
                resp(httpSession, new Response(Response.INTERNAL_ERROR, Response.EMPTY));
            }
        }, executor).exceptionally(exception -> {
            logger.error("Ошибка при выполнении операции DELETE (DAO): ", exception);
            return null;
        });
    }

    private CompletableFuture<Boolean> runAsyncIfReplicasContainNode(
            @NotNull final List<String> replicas,
            @NotNull final Runnable runnable) {
        return CompletableFuture.supplyAsync(() -> {
            if (replicas.contains(topology.recogniseMyself())) {
                runnable.run();
                return true;
            }
            return false;
        }, executor);
    }

    private void getSuccessAndSendIfReachedExpected(
            @NotNull final HttpSession httpSession,
            @NotNull final MetaInfoRequest mir,
            @NotNull final List<String> replicas,
            @NotNull final List<HttpResponse<byte[]>> responses,
            final int statusCode) {
        final Predicate<HttpResponse<byte[]>> successPut = r -> r.statusCode() == statusCode;
        final int acks = getNumberOfSuccessfulResponses(getStartAcks(replicas), responses, successPut);
        final String response = statusCode == 201
                ? Response.CREATED : Response.ACCEPTED;
        sendResponseIfExpectedAcksReached(
                acks, mir.getReplicaFactor().getAck(), new Response(response, Response.EMPTY), httpSession);
    }

    private int getNumberOfSuccessfulResponses(
            final int startAcks,
            @NotNull final List<HttpResponse<byte[]>> responses,
            @NotNull final Predicate<HttpResponse<byte[]>> ok) {
        int acks = startAcks;
        for (final HttpResponse<byte[]> response : responses) {
            if (ok.test(response)) {
                acks++;
            }
        }
        return acks;
    }

    private void sendResponseIfExpectedAcksReached(
            final int myAcks,
            final int acksThreshold,
            @NotNull final Response response,
            @NotNull final HttpSession httpSession) {
        if (myAcks >= acksThreshold) {
            resp(httpSession, response);
        } else {
            resp(httpSession, new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
        }
    }
}
