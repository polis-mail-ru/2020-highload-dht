package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.stasyanoi.DAOImpl;
import ru.mail.polis.service.stasyanoi.CustomExecutor;
import ru.mail.polis.service.stasyanoi.Merger;
import ru.mail.polis.service.stasyanoi.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConstantsServer extends HttpServer {

    protected static final String TRUE = "true";
    protected static final String REPLICAS = "replicas";
    protected static final String SHOULD_REPLICATE = "reps";
    protected Map<Integer, String> nodeIndexToUrlMapping;
    protected int nodeAmount;
    protected int thisNodeIndex;
    protected DAO dao;
    protected Merger merger;
    protected Util util;
    protected CustomExecutor executorService = CustomExecutor.getExecutor();
    protected Map<String, HttpClient> httpClientMap;
    protected java.net.http.HttpClient asyncHttpClient;
    protected Logger logger = LoggerFactory.getLogger(ConstantsServer.class);

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
        this.nodeAmount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        urls.sort(String::compareTo);

        this.nodeIndexToUrlMapping = new TreeMap<>();

        asyncHttpClient = java.net.http.HttpClient.newBuilder().build();
        for (int i = 0; i < urls.size(); i++) {
            nodeIndexToUrlMapping.put(i, urls.get(i));
            httpClientMap.put(urls.get(i), new HttpClient(new ConnectionString(urls.get(i))));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                thisNodeIndex = i;
            }
        }
        if (dao instanceof DAOImpl) {
            this.util = ((DAOImpl) dao).getUtil();
        } else {
            throw new RuntimeException("Not the proper DAOimpl");
        }
        this.dao = dao;
        this.merger = new Merger(this.util);
    }
}
