package ru.mail.polis.service.stasyanoi;

import one.nio.http.*;
import ru.mail.polis.dao.DAO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FrameServer extends HttpServer {

    protected Map<Integer, String> nodeMapping;
    protected int nodeCount;
    protected int nodeNum;
    protected DAO dao;
    protected ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public FrameServer(final DAO dao,
                       final HttpServerConfig config,
                       final Set<String> topology) throws IOException {
        super(config);
        this.nodeCount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();
        for (int i = 0; i < urls.size(); i++) {
            nodeMappingTemp.put(i, urls.get(i));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
        }
        this.nodeMapping = nodeMappingTemp;
        this.dao = dao;
    }

    @Override
    public synchronized void start() {
        super.start();
        dao.open();
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

    /**
     * Default handler for unmapped requests.
     *
     * @param request - unmapped request
     * @param session - session object
     * @throws IOException - if input|output exceptions occur within the method
     */
    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        final Response response = Util.getResponseWithNoBody(Response.BAD_REQUEST);
        session.sendResponse(response);
    }
}
