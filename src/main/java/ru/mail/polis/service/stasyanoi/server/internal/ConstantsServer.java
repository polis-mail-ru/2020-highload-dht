package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpClient;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.net.ConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomExecutor;

import java.io.IOException;
import java.util.*;

public class ConstantsServer extends HttpServer {

    protected static final String TRUE_VAL = "true";
    protected static final String REPS = "reps";
    protected static final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");
    protected Logger logger = LoggerFactory.getLogger(ConstantsServer.class);
    protected Map<Integer, String> nodeMapping;
    protected int nodeCount;
    protected int nodeNum;
    protected DAO dao;
    protected CustomExecutor executorService = CustomExecutor.getExecutor();
    protected Map<String, HttpClient> httpClientMap;

    /**
     * Fields server.
     *
     * @param dao - dao.
     * @param config - config.
     * @param topology - topology.
     * @throws IOException - IOException.
     */
    public ConstantsServer(final DAO dao,
                           final HttpServerConfig config,
                           final Set<String> topology) throws IOException {
        super(config);
        this.nodeCount = topology.size();
        final ArrayList<String> urls = new ArrayList<>(topology);
        urls.sort(String::compareTo);

        final Map<Integer, String> nodeMappingTemp = new TreeMap<>();
        final Map<String, HttpClient> clients = new HashMap<>();
        for (int i = 0; i < urls.size(); i++) {
            nodeMappingTemp.put(i, urls.get(i));
            clients.put(urls.get(i), new HttpClient(new ConnectionString(urls.get(i))));
            if (urls.get(i).contains(String.valueOf(super.port))) {
                nodeNum = i;
            }
        }
        this.httpClientMap = clients;
        this.nodeMapping = nodeMappingTemp;
        this.dao = dao;
    }

}
