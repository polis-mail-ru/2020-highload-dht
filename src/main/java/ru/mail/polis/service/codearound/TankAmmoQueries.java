package ru.mail.polis.service.codearound;

//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  class to enable implementing of methods to advance highload cluster stress tests
 *  using both Yandex Tank and Overload service.
 */
public final class TankAmmoQueries {

    private static final Logger LOGGER = Logger.getLogger(TankAmmoQueries.class.getName());
    private static final int KEY_LENGTH = 256;
    private static final String CRLF = "\r\n";
    private static final String tagOfGet = " GET\n";
    private static final String tagOfPut = " PUT\n";

    /**
     * class instance const.
     */
    private TankAmmoQueries() {

    }

    /**
     * spawns quasi-random values to be pushed to the store by PUT requests.
     *
     * @return pool of keys written into plain byte array
     */
    private static byte[] getRand() {
        final byte[] rand = new byte[KEY_LENGTH];
        ThreadLocalRandom.current().nextBytes(rand);
        return rand;
    }

    /**
     * generates PUT requests without key duplicates (query option #1).
     *
     * @param numOfKeys - number of keys to be available for pushing into the storage via PUT requests
     */
    private static void putRandKey(final long numOfKeys) throws IOException {
        for (long i = 0; i < numOfKeys; i++) {
            putKey(Long.toString(i));
        }
    }

    /**
     * generates PUT requests featured to resolving partial overwrites among keys stored (query option #2).
     *
     * @param numOfKeys - number of keys to be available for pushing into the storage via PUT requests
     */
    private static void putWhenOverwriting(final long numOfKeys) throws IOException {
        long initKey = 0;
        for (long i = 0; i < numOfKeys; i++) {
            if (ThreadLocalRandom.current().nextInt(10) == 1) {
                final String key = Long.toString(ThreadLocalRandom.current().nextLong(initKey));
                putKey(key);
            } else {
                putKey(Long.toString(initKey));
                initKey++;
            }
        }
    }

    /**
     * generates GET requests consistently with normal distribution law (query option #3).
     *
     * @param numOfKeys - number of keys to be available for fetching from the storage via GET requests
     */
    private static void getKeyNormalDist(final int numOfKeys) throws IOException {
        for (long i = 0; i < numOfKeys; i++) {
            getKey(Long.toString(ThreadLocalRandom.current().nextLong(numOfKeys)));
        }
    }

    /**
     * generates GET requests assumed to apply test reading of data in the way it follows flash tendency
     * to handle temporarily rising number of requests for the newest added / refreshed contents (query option #4).
     *
     * @param numOfKeys - number of keys to be available for fetching from the storage via GET requests
     * @param numOfEntries - size of record pool given to select recently added entries
     */
    private static void getLatestKey(final long numOfKeys, final long numOfEntries) throws IOException {
        long key;
        int position;
        for (long i = 0; i < numOfKeys; i++) {
            position = ThreadLocalRandom.current().nextInt(0, 10);

            if (position == 0) {
                key = ThreadLocalRandom.current().nextLong(0, numOfEntries / 2);
            } else if (position == 1) {
                key = ThreadLocalRandom.current().nextLong(numOfEntries / 2, numOfEntries / 2 + numOfEntries / 4);
            } else {
                key = ThreadLocalRandom.current().nextLong(numOfEntries / 2 + numOfEntries / 4, numOfEntries);
            }
            getKey(Long.toString(key));
        }
    }

    /**
     * generates mixed (PUT has a share equal one of GET, i.e. just 50%) requests consistently
     * with continuous distribution law (query option #5).
     *
     * @param numOfKeys - number of keys to be available for pushing into / fetching from
     *                  the storage via respective requests
     */
    private static void mixPutGetLoad(final long numOfKeys) throws IOException {
        long initKey = 0;
        boolean isPutRequest;
        putKey(Long.toString(initKey));

        for (long i = 1; i < numOfKeys; i++) {
            isPutRequest = ThreadLocalRandom.current().nextBoolean();
            if (isPutRequest) {
                putKey(Long.toString(initKey++));
            } else {
                getKey(Long.toString(ThreadLocalRandom.current().nextLong(initKey)));
            }
        }
    }

    /**
     * issues common request by any GET testing option determined before running any supported test .
     *
     * @param keyStr - key written into a String given
     */
    private static void getKey(final String keyStr) throws IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try(Writer writer = new OutputStreamWriter(outStream, StandardCharsets.US_ASCII)) {
            writer.write("GET /v0/entity?id=" + keyStr + " HTTP/1.1" + CRLF);
            writer.write(CRLF);
        }
        System.out.write(Integer.toString(outStream.size()).getBytes(StandardCharsets.US_ASCII));
        System.out.write(tagOfGet.getBytes(StandardCharsets.US_ASCII));
        outStream.writeTo(System.out);
        System.out.write(CRLF.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * issues common request by any PUT testing option determined before running any supported test .
     *
     * @param keyStr - key written into a String given
     */
    private static void putKey(final String keyStr) throws IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final byte[] value = getRand();

        try(Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8)) {
            writer.write("PUT /v0/entity?id=" + keyStr + " HTTP/1.1" + CRLF);
            writer.write("Content-Length: " + value.length + CRLF);
            writer.write(CRLF);
        }
        outStream.write(value);
        System.out.write(Integer.toString(outStream.size()).getBytes(StandardCharsets.UTF_8));
        System.out.write(tagOfPut.getBytes(StandardCharsets.UTF_8));
        outStream.writeTo(System.out);
        System.out.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * initializes determining a test query and (following one) handler option using cmd interface.
     *
     * @param args - cmd arguments to be specified before running test upon option pick
     */
    public static void main(String [] args) throws IOException {

        if (args.length < 2 || args.length > 3) {
            LOGGER.log(Level.SEVERE, "Given inconsistent number of arguments before program running");
            throw new IllegalArgumentException("Given inconsistent number of arguments before program running");
        }

        final String querySelected = args[0];
        final int requestPool = Integer.parseInt(args[1]);

        switch (querySelected) {
            case "1":
                putRandKey(requestPool);
                break;
            case "2":
                putWhenOverwriting(requestPool);
                break;
            case "3":
                getKeyNormalDist(requestPool);
                break;
            case "4":
                final long numOfEntries = Long.parseLong(args[2]);
                getLatestKey(requestPool, numOfEntries);
                break;
            case "5":
                mixPutGetLoad(requestPool);
                break;
            default:
                throw new UnsupportedOperationException("Option " + querySelected + " not supported yet");
        }
    }
}
