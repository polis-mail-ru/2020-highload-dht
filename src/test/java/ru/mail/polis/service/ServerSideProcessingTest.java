package ru.mail.polis.service;

import com.google.common.base.Splitter;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Files;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ServerSideProcessingTest extends ClusterTestBase {
    private DAO dao0;
    private DAO dao1;
    private DAO dao2;
    private File dir0;
    private File dir1;
    private File dir2;
    private Service storage0;
    private Service storage1;
    private Service storage2;
    private static final int[] PORTS = {8080, 8081, 8082};
    private static final String URL = "http://localhost:";

    @BeforeEach
    void beforeEach() throws Exception {

        final Set<String> topology = new HashSet<>(3);
        for (final int port : PORTS) {
            topology.add(URL + port);

        }
        nodes = topology.toArray(new String[0]);

        dir0 = Files.createTempDirectory();
        dao0 = DAOFactory.create(dir0);
        storage0 = ServiceFactory.create(PORTS[0], dao0, topology);
        storage0.start();
        dir1 = Files.createTempDirectory();
        dao1 = DAOFactory.create(dir1);
        storage1 = ServiceFactory.create(PORTS[1], dao1, topology);
        storage1.start();
        dir2 = Files.createTempDirectory();
        dao2 = DAOFactory.create(dir2);
        storage2 = ServiceFactory.create(PORTS[2], dao2, topology);
        storage2.start();
        services = new Service[3];
        services[0] = storage0;
        services[1] = storage1;
        services[2] = storage2;
    }

    @AfterEach
    void afterEach() throws IOException {
        stop(0);
        dao0.close();
        Files.recursiveDelete(dir0);
        stop(1);
        dao1.close();
        Files.recursiveDelete(dir1);
        stop(2);
        dao2.close();
        Files.recursiveDelete(dir2);
    }

    @Test
    void topTenKeys() throws Exception {
        final String[] keys = getKeys();
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final byte[] value = randomValue();
            final int node = getRandomNode();
            upsert(node, key, value, 1, 1);
        }

        final List<Integer> intKeys = new ArrayList<>();
        for (int j = 0; j < keys.length; j++) {
            intKeys.add(Integer.parseInt(keys[j]));
        }
        // sort keys
        intKeys.sort(Integer::compareTo);
        intKeys.sort(Comparator.reverseOrder());


        List<Integer> topTen = new ArrayList<>();
        // get topTen for check
        for (int i = 0; i < 10; i++) {
            topTen.add(intKeys.get(i));
        }

        // send JSCode to node
        byte[] jsCode = getJSCode();
        final Response result = postToNode(0, jsCode);
        assertEquals(200, result.getStatus());

        final List<String> keysStringJS = new ArrayList<>();
        keysStringJS.addAll(Splitter.on(',').splitToList(result.getBodyUtf8()));


        final List<Integer> topTenFromJs = new ArrayList<>();

        for (int j = 0; j < keysStringJS.size(); j++) {
            topTenFromJs.add(Integer.parseInt(keysStringJS.get(j)));
        }
        topTenFromJs.sort(Comparator.reverseOrder());

        assertEquals(topTen, topTenFromJs);

    }

    Response postToNode(final int node,
                        @NotNull final byte[] codeJS) throws Exception {
        return client(node).post("/v0/jscode", codeJS);
    }

    private static String[] getKeys() {
        final Random random = new Random();
        final String[] keys = new String[100];
        for (int i = 0; i < 100; i++) {
            keys[i] = String.valueOf(random.nextInt(50000));
        }
        return keys;
    }

    private static int getRandomNode() {
        final Random random = new Random();
        return random.nextInt(3);
    }

    private byte[] getJSCode() throws IOException {
        final ClassLoader classLoader = ServerSideProcessingTest.class.getClassLoader();
        final InputStream codeJsInBytes = classLoader.getResourceAsStream("TenLargestClusterKeys.js");
        byte[] jsCode = codeJsInBytes.readAllBytes();
        return jsCode;
    }

    @Override
    int getClusterSize() {
        return 0;
    }

}