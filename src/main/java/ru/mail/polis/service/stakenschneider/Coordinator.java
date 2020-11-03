package ru.mail.polis.service.stakenschneider;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.RequestProcessor;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.RocksDAO;
import ru.mail.polis.dao.TimestampRecord;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;

class Coordinator {
    private final RocksDAO dao;
    private final Nodes nodes;
    private static final HttpClient client = HttpClient.newHttpClient();
    private final RequestProcessor.MyConsumer<HttpSession,
            List<CompletableFuture<Void>>,
            Integer,
            AtomicInteger,
            Boolean> processError = (session, futureList, neededAcks, receivedAcks, proxied) -> {
        if (receivedAcks.getAcquire() < neededAcks
                && !(proxied && receivedAcks.getAcquire() == 1))
            try {
                session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
            } catch (IOException e) {
                session.close();
            }
    };

    private void checkResponses(final List<CompletableFuture<Void>> futureList,
                                final HttpSession session,
                                final Integer neededAcks,
                                final AtomicInteger receivedAcks,
                                final Boolean proxied) {
        CompletableFuture.allOf(futureList.toArray(CompletableFuture<?>[]::new))
                .thenAccept(x -> processError.accept(session, futureList, neededAcks, receivedAcks, proxied))
                .exceptionally(x -> {
                            processError.accept(session, futureList, neededAcks, receivedAcks, proxied);
                            return null;
                        }
                );
    }

    private void processPutAndDeleteRequest(final List<HttpRequest> requests,
                                            final AtomicInteger receivedAcks,
                                            final Supplier<Response> successResponse,
                                            final HttpSession session,
                                            final Integer neededAcks) {

        final boolean proxied = requests.isEmpty();
        final List<CompletableFuture<Void>> futureList = requests.stream()
                .map(request -> client.sendAsync(request, ofByteArray())
                        .thenAccept(response -> {
                            if (response.statusCode() == successResponse.get().getStatus())
                                receivedAcks.incrementAndGet();
                            sendResult(successResponse, neededAcks, receivedAcks, session, false);
                        }))
                .collect(Collectors.toList());
        checkResponses(futureList, session, neededAcks, receivedAcks, proxied);
    }


    private void sendResult(final Supplier<Response> processResponse,
                            final Integer neededAcks,
                            final AtomicInteger receivedAcks,
                            final HttpSession session,
                            final boolean proxied) {
        if (receivedAcks.getAcquire() >= neededAcks || proxied) {
            try {
                session.sendResponse(processResponse.get());
            } catch (IOException e) {
                session.close();
            }
        }
    }

    /**
     * Create the cluster coordinator instance.
     *
     */
    Coordinator(@NotNull final Nodes nodes,
                 @NotNull final DAO dao) {
        this.dao = (RocksDAO) dao;
        this.nodes = nodes;
    }

    /**
     * Coordinate the delete among all clusters.
     *
     */
    private void coordinateDelete(final String[] replicaNodes,
                                  @NotNull final HttpSession session,
                                  @NotNull final Request rqst,
                                  final int acks) {
        final var model = new RequestProcessor.ProcessRequestModel(replicaNodes, rqst, acks);
        final Function<HttpRequest.Builder, HttpRequest.Builder> methodDefiner = HttpRequest.Builder::DELETE;
        final Supplier<Response> successResponse = () -> new Response(Response.ACCEPTED, Response.EMPTY);
        if (model.uris.remove(nodes.getId())) {
            try {
                dao.removeRecordWithTimestamp(model.key);
                model.recievidAcks.incrementAndGet();
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                } catch (IOException exp) {
                    session.close();
                }
            }
        }
        sendResult(successResponse, acks, model.recievidAcks, session, model.proxied);
        final List<HttpRequest> requests = RequestProcessor.createRequests(model.uris, rqst, methodDefiner);
        if (!model.uris.isEmpty()) {
            processPutAndDeleteRequest(requests, model.recievidAcks, successResponse, session, acks);
        }
    }

    /**
     * Coordinate the put among all clusters.
     *
     */
    private void coordinatePut(final String[] replicaNodes,
                               @NotNull final HttpSession session,
                               @NotNull final Request rqst,
                               final int acks) {
        final var model = new RequestProcessor.ProcessRequestModel(replicaNodes, rqst, acks);
        final Function<HttpRequest.Builder, HttpRequest.Builder> methodDefiner =
                x -> x.PUT(HttpRequest.BodyPublishers.ofByteArray(rqst.getBody()));
        final Supplier<Response> successResponse = () -> new Response(Response.CREATED, Response.EMPTY);
        if (model.uris.remove(nodes.getId())) {
            try {
                dao.upsertRecordWithTimestamp(model.key, ByteBuffer.wrap(rqst.getBody()));
                model.recievidAcks.incrementAndGet();
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                } catch (IOException exp) {
                    session.close();
                }
            }
        }
        this.sendResult(successResponse, acks, model.recievidAcks, session, model.proxied);
        final List<HttpRequest> requests = RequestProcessor.createRequests(model.uris, rqst, methodDefiner);
        if (!model.uris.isEmpty()) {
            processPutAndDeleteRequest(requests, model.recievidAcks, successResponse, session, acks);
        }
    }

    /**
     * Coordinate the get among all clusters.
     *
     */
    private void coordinateGet(final String[] replicaNodes,
                               @NotNull final HttpSession session,
                               @NotNull final Request rqst,
                               final int acks) throws IOException {
        final var model = new RequestProcessor.ProcessRequestModel(replicaNodes, rqst, acks);
        final Function<HttpRequest.Builder, HttpRequest.Builder> methodDefiner = HttpRequest.Builder::GET;
        if (model.uris.remove(nodes.getId())) {
            try {
                getTimestampRecordFromLocalDao(model.key, model.responses);
                model.recievidAcks.incrementAndGet();
            } catch (IOException e) {
                try {
                    session.sendResponse(new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY));
                } catch (IOException exp) {
                    session.close();
                }
            }
        }
        this.sendResult(() -> processResponses(model.responses, model.proxied), acks, model.recievidAcks, session, model.proxied);
        if (!model.uris.isEmpty()) {
            final List<HttpRequest> requests = RequestProcessor.createRequests(model.uris, rqst, methodDefiner);
            final List<CompletableFuture<Void>> futureList = requests.stream()
                    .map(request -> client.sendAsync(request, ofByteArray())
                            .thenAccept(response -> {
                                checkGetProxiedResponses(response, model, session);
                            }))
                    .collect(Collectors.toList());
            checkResponses(futureList, session, acks, model.recievidAcks, model.proxied);
        }
    }

    private void checkGetProxiedResponses(final HttpResponse<byte[]> response,
                                          final RequestProcessor.ProcessRequestModel model,
                                          final HttpSession session) {
        if (response.statusCode() == 404 && response.body().length == 0) {
            model.responses.add(TimestampRecord.getEmpty());
        }
        if (response.statusCode() == 500) return;
        model.responses.add(TimestampRecord.fromBytes(response.body()));
        model.recievidAcks.incrementAndGet();
        this.sendResult(() -> processResponses(model.responses, model.proxied),
                model.neededAcks,
                model.recievidAcks,
                session,
                model.proxied);
    }

    private void getTimestampRecordFromLocalDao(final ByteBuffer key,
                                                final List<TimestampRecord> responses) throws IOException {
        try {
            final var record = TimestampRecord.fromBytes(copyAndExtractWithTimestampFromByteBuffer(key));
            responses.add(record);
        } catch (NoSuchElementException exp) {
            responses.add(TimestampRecord.getEmpty());
        }
    }

    private Response processResponses(final List<TimestampRecord> responses,
                                      final boolean proxied) {
        final TimestampRecord mergedResp = TimestampRecord.merge(responses);
        if (mergedResp.isValue()) {
            if (proxied) {
                return new Response(Response.OK, mergedResp.toBytes());
            } else {
                return new Response(Response.OK, mergedResp.getValueAsBytes());
            }
        } else if (mergedResp.isDeleted()) {
            return new Response(Response.NOT_FOUND, mergedResp.toBytes());
        } else {
            return new Response(Response.NOT_FOUND, Response.EMPTY);
        }
    }

    private byte[] copyAndExtractWithTimestampFromByteBuffer(@NotNull final ByteBuffer key) throws IOException {
        final TimestampRecord res = dao.getRecordWithTimestamp(key);
        if (res.isEmpty()) {
            throw new NoSuchElementException("Element not found!");
        }
        return res.toBytes();
    }

    /**
     * Coordinate the request among all clusters.
     *
     */
    void coordinateRequest(final String[] replicaClusters,
                           final Request request,
                           final int acks,
                           final HttpSession session) throws IOException {
        try {
            switch (request.getMethod()) {
                case Request.METHOD_GET:
                    coordinateGet(replicaClusters, session, request, acks);
                    break;
                case Request.METHOD_PUT:
                    coordinatePut(replicaClusters, session, request, acks);
                    break;
                case Request.METHOD_DELETE:
                    coordinateDelete(replicaClusters, session, request, acks);
                    break;
                default:
                    session.sendError(Response.METHOD_NOT_ALLOWED, "Wrong method");
                    break;
            }
        } catch (IOException e) {
            session.sendError(Response.GATEWAY_TIMEOUT, e.getMessage());
        }
    }
}
