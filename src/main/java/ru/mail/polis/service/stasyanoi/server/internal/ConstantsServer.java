package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomExecutor;
import ru.mail.polis.service.stasyanoi.server.CustomServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConstantsServer extends HttpServer {

    protected static final String TRUE_VAL = "true";
    protected static final String REPS = "reps";
    protected final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");
    protected Map<Integer, String> nodeMapping;
    protected int nodeCount;
    protected int nodeNum;
    protected DAO dao;
    protected CustomExecutor executorService = CustomExecutor.getExecutor();
    protected java.net.http.HttpClient asyncHttpClient;
    protected Logger logger = LoggerFactory.getLogger(CustomServer.class);

    /**
     * Fields server.
     *
     * @param dao - dao.
     * @param config - config.
     * @param topology - topology.
     * @throws IOException - IOException.
     */
    public ConstantsServer(final DAO dao, final HttpServerConfig config, final Set<String> topology)
            throws IOException {
        super(config);
        this.nodeCount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        urls.sort(String::compareTo);

        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();

        asyncHttpClient = java.net.http.HttpClient.newBuilder()
                .build();
        for (int i = 0; i < urls.size(); i++) {
            nodeMappingTemp.put(i, urls.get(i));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
        }
        this.nodeMapping = nodeMappingTemp;
        this.dao = dao;
    }
}
