package ru.mail.polis.service.alexander.marashov;

import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzer;
import ru.mail.polis.service.alexander.marashov.analyzers.ResponseAnalyzerGet;
import ru.mail.polis.service.alexander.marashov.analyzers.SimpleResponseAnalyzer;
import ru.mail.polis.service.alexander.marashov.topologies.Topology;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ru.mail.polis.service.alexander.marashov.ServiceImpl.PROXY_HEADER;
import static ru.mail.polis.service.alexander.marashov.ServiceImpl.PROXY_HEADER_VALUE;

public class ResponseManager {

    private static final Logger log = LoggerFactory.getLogger(ResponseManager.class);
    private static final String WAITING_INTERRUPTED = "Responses waiting was interrupted";

    private final Topology<String> topology;
    private final Map<String, HttpClient> nodeToClient;
    private final DaoManager daoManager;
    private final ExecutorService proxyExecutor;

    /**
     * Object for managing responses.
     * @param dao - DAO instance.
     * @param topology - object with cluster info.
     * @param proxyTimeoutValue - proxy timeout value in milliseconds.
     * @param proxyExecutor - executorService instance where proxy requests should be executed.
     */
    public ResponseManager(
            final DAO dao,
            final Topology<String> topology,
            final int proxyTimeoutValue,
            final ExecutorService proxyExecutor
    ) {
        this.daoManager = new DaoManager(dao);
        this.topology = topology;
        this.nodeToClient = topology.clientsToOtherNodes(proxyTimeoutValue);
        this.proxyExecutor = proxyExecutor;
    }

    /**
     * Executes a get request to the DAO.
     * @param validParams - validated parameters object.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public Response get(final ValidatedParameters validParams, final Request request) {
        final String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            return daoManager.rowGet(validParams.key);
        }

        request.addHeader(PROXY_HEADER_VALUE);
        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);
        final ResponseAnalyzerGet valueAnalyzer =
                new ResponseAnalyzerGet(validParams.ack, validParams.from);

        iterateOverNodes(
                primaries,
                () -> {
                    final Response response = daoManager.rowGet(validParams.key);
                    valueAnalyzer.accept(response);
                },
                (String primary) -> {
                    Response response;
                    try {
                        response = nodeToClient.get(primary).invoke(request);
                    } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                        response = null;
                        log.error("Get: Error sending request to node {}", primary, e);
                    }
                    valueAnalyzer.accept(response);
                }
        );

        try {
            valueAnalyzer.await(1000L, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            log.error(WAITING_INTERRUPTED, e);
            Thread.currentThread().interrupt();
        }

        return valueAnalyzer.getResult();
    }

    /**
     * Executes a put request to the DAO.
     * @param validParams - validated parameters object.
     * @param value - byte buffer with value to put.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public Response put(final ValidatedParameters validParams, final ByteBuffer value, final Request request) {
        final String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            return daoManager.put(validParams.key, value);
        }

        request.addHeader(PROXY_HEADER_VALUE);
        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);
        final ResponseAnalyzer<Boolean> responseAnalyzer = new SimpleResponseAnalyzer(
                validParams.ack,
                validParams.from,
                201,
                Response.CREATED
        );

        iterateOverNodes(
                primaries,
                () -> {
                    final Response response = daoManager.put(validParams.key, value);
                    responseAnalyzer.accept(response);
                },
                (primary) -> {
                    Response response = null;
                    try {
                        response = nodeToClient.get(primary).invoke(request);
                    } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                        log.error("Upsert: Error sending request to node {}", primary, e);
                    }
                    responseAnalyzer.accept(response);
                }
        );

        try {
            responseAnalyzer.await(1000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            log.error(WAITING_INTERRUPTED);
            Thread.currentThread().interrupt();
        }
        return responseAnalyzer.getResult();

    }

    /**
     * Executes a delete request to the DAO.
     * @param validParams - validated parameters object.
     * @param request - original user's request.
     * @return response - the final result of the completed request.
     */
    public Response delete(final ValidatedParameters validParams, final Request request) {
        final String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            return daoManager.delete(validParams.key);
        }

        request.addHeader(PROXY_HEADER_VALUE);
        final String[] primaries = topology.primariesFor(validParams.key, validParams.from);
        final ResponseAnalyzer<Boolean> responseAnalyzer = new SimpleResponseAnalyzer(
                validParams.ack,
                validParams.from,
                202,
                Response.ACCEPTED
        );

        iterateOverNodes(
                primaries,
                () -> {
                    final Response response = daoManager.delete(validParams.key);
                    responseAnalyzer.accept(response);
                },
                (primary) -> {
                    Response response = null;
                    try {
                        response = nodeToClient.get(primary).invoke(request);
                    } catch (final InterruptedException | PoolException | IOException | HttpException e) {
                        log.error("Delete: Error sending request to node {}", primary, e);
                    }
                    responseAnalyzer.accept(response);
                }
        );

        try {
            responseAnalyzer.await(1000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            log.error(WAITING_INTERRUPTED);
            Thread.currentThread().interrupt();
        }

        return responseAnalyzer.getResult();
    }

    private void iterateOverNodes(final String[] nodes, final Runnable localTask, final Consumer<String> proxyTask) {
        for (final String node : nodes) {
            if (topology.isLocal(node)) {
                proxyExecutor.execute(localTask);
            } else {
                proxyExecutor.execute(() -> proxyTask.accept(node));
            }
        }
    }

    /**
     * Closes the daoManager and other node's clients.
     */
    public void clear() {
        for (final HttpClient client : nodeToClient.values()) {
            client.clear();
        }
        daoManager.close();
    }
}
