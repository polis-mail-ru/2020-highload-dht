package ru.mail.polis.service.stasyanoi.server.internal;

import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.stasyanoi.CustomExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConstantsServer extends HttpServer {

    protected final String TRUE_VAL = "true";
    protected final String REPS = "reps";
    protected final List<String> replicationDefaults = Arrays.asList("1/1", "2/2", "2/3", "3/4", "3/5");
    protected Map<Integer, String> nodeMapping;
    protected int nodeCount;
    protected int nodeNum;
    protected DAO dao;
    protected CustomExecutor executorService = CustomExecutor.getExecutor();


    public ConstantsServer(final DAO dao,
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

}
