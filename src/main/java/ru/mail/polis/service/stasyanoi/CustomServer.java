package ru.mail.polis.service.stasyanoi;

import com.google.common.net.HttpHeaders;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.Mapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

public class CustomServer extends HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(CustomServer.class);

    private final Map<Integer, String> nodeMapping;
    private final int nodeCount;
    private final DAO dao;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public CustomServer(final DAO dao,
                        final HttpServerConfig config, Set<String> topology) throws IOException {
        super(config);
        this.nodeCount = topology.size();
        ArrayList<String> urls = new ArrayList<>(topology);
        urls.sort(String::compareTo);

        Map<Integer, String> nodeMapping = new HashMap<>();

        for (int i = 0; i < urls.size(); i++) {
            nodeMapping.put(i, urls.get(i));
        }

        this.nodeMapping = nodeMapping.entrySet().stream()
                .filter(integerStringEntry -> !integerStringEntry.getValue()
                        .contains(String.valueOf(super.port))).collect(Collectors.
                toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.dao = dao;
    }

    /**
     * Get a record by key.
     *
     * @param idParam - key.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_GET)
    public void get(final @Param("id") String idParam, final HttpSession session) {
        executorService.execute(() -> {
            try {
                getInternal(idParam, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void getInternal(final String idParam,
                             final HttpSession session) throws IOException {
        final Response responseHttp;
        //check id param
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            //get id as aligned byte buffer
            final ByteBuffer id = Mapper.fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            //get the response from db
            responseHttp = getResponseIfIdNotNull(id);
        }

        session.sendResponse(responseHttp);
    }

    @NotNull
    private Response getResponseIfIdNotNull(final ByteBuffer id) throws IOException {
        try {
            final ByteBuffer body = dao.get(id);
            final byte[] bytes = Mapper.toBytes(body);
            return Response.ok(bytes);
        } catch (NoSuchElementException e) {
            //if not found then 404
            return getResponseWithNoBody(Response.NOT_FOUND);
        }
    }

    @NotNull
    private Response getResponseWithNoBody(final String requestType) {
        final Response responseHttp = new Response(requestType);
        responseHttp.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
        return responseHttp;
    }

    /**
     * Create or update a record.
     *
     * @param idParam - key of the record.
     * @param request - request with the body.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_PUT)
    public void put(final @Param("id") String idParam,
                    final Request request,
                    final HttpSession session) {
        executorService.execute(() -> {
            try {
                putInternal(idParam, request, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void putInternal(final String idParam, final Request request,
                             final HttpSession session) throws IOException {
        final Response responseHttp;
        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer key = Mapper.fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            final ByteBuffer value = Mapper.fromBytes(request.getBody());
            dao.upsert(key, value);
            responseHttp = getResponseWithNoBody(Response.CREATED);
        }
        session.sendResponse(responseHttp);
    }

    /**
     * Delete a record.
     *
     * @param idParam - key of the record to delete.
     */
    @Path("/v0/entity")
    @RequestMethod(METHOD_DELETE)
    public void delete(final @Param("id") String idParam,
                       final HttpSession session) {
        executorService.execute(() -> {
            try {
                deleteInternal(idParam, session);
            } catch (IOException e) {
                sendErrorInternal(session, e);
            }
        });
    }

    private void deleteInternal(final String idParam,
                                final HttpSession session) throws IOException {
        final Response responseHttp;

        if (idParam == null || idParam.isEmpty()) {
            responseHttp = getResponseWithNoBody(Response.BAD_REQUEST);
        } else {
            final ByteBuffer key = Mapper.fromBytes(idParam.getBytes(StandardCharsets.UTF_8));
            dao.remove(key);
            responseHttp = getResponseWithNoBody(Response.ACCEPTED);
        }
        session.sendResponse(responseHttp);
    }

    private void sendErrorInternal(final HttpSession session,
                                   final IOException e) {
        try {
            logger.error(e.getMessage(), e);
            session.sendError("500", e.getMessage());
        } catch (IOException exception) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Default handler for unmapped requests.
     *
     * @param request - unmapped request
     * @param session - session object
     * @throws IOException - if input|output exceptions occur within the method
     */
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = getResponseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }

    /**
     * Status check.
     *
     * @return Response with status.
     */
    @Path("/v0/status")
    @RequestMethod(METHOD_GET)
    public Response status() {
        return getResponseWithNoBody(Response.OK);
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        try {
            dao.close();
            executorService.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
