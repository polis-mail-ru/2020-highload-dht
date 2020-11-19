package ru.mail.polis.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.mail.polis.service.ivanovandrey.RandezvouzTopology;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RendezvousTest {
    private final RandezvouzTopology randezvouzTopology = new RandezvouzTopology(8080);
    final Set<String> topology = new HashSet<>();
    final int numberOfRequests = 1000000;
    final int inaccuracy = 5;

    @NotNull
    protected static String randomId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    /**
     * Creating an int array with port numbers for nodes.
     * @param count - count.
     */
    int[] createPorts(int count) {
        Set<Integer> generated = new HashSet<Integer>();
        Random r = new Random();
        while(generated.size() < count) {
            generated.add(r.nextInt(count) + 1);
        }
        return generated.stream().mapToInt(i -> i).toArray();
    }

    /**
     * @param nodes - Requests for a single key (Considering replicas).
     * @param ports - Array of all ports for the nodes.
     * @param results - Current results of the number of requests for each node.
     */
    int[] copmare(final Set<String> nodes, final int[] ports, int[] results) {
        for(final String requestNode : nodes) {
            for(int i = 0; i < ports.length; i++ ) {
                if(requestNode.equals(String.valueOf(ports[i]))) {
                    results[i]++;
                }
            }
        }
        return results;
    }

    /**
     * @param results - Results array for analysis.
     //* @param numberOfReplicas - number of replicas for each request.
     */
    boolean check(int[] results, int numberOfRequests){
        int expectedResult = numberOfRequests / results.length;
        int permissibleError = expectedResult * inaccuracy / 100;
        for (int result : results) {
            if(result - expectedResult > permissibleError) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checking for distribution.
     * @param numberOfPorts - number of ports.
     */
    void distribution(int numberOfPorts) {
        final Set<String> requestNodes = new HashSet<String>();
        int replicasCount = 0;
        int numberOfReplicas;
        int[] ports = createPorts(numberOfPorts);
        int[] results = new int[ports.length];
        for(final int port : ports) {
            topology.add("" + port);
        }
        for(int i = 0; i < numberOfRequests ; i++) {
            Random r = new Random();
            numberOfReplicas = r.nextInt(numberOfPorts) + 1;
            replicasCount += numberOfReplicas;
            requestNodes.addAll(randezvouzTopology.getNodes(topology, randomId(), numberOfReplicas));
            results = copmare(requestNodes, ports, results);
            requestNodes.clear();
        }
        assertTrue(check(results, replicasCount));
    }

    @Test
    void t1() {
        distribution(3);
    }

    @Test
    void t2() {
        distribution(10);
    }

    @Test
    void t3() {
        distribution(50);
    }

    @RepeatedTest(20)
    void random() {
        Random r = new Random();
        int ports = (r.nextInt(100) + 1);
        distribution(ports);
    }
}
