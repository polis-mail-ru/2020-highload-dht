package ru.mail.polis.service.codearound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *  class pools up implementations of methods to carry out highload cluster stress tests
 *  using both Yandex Tank and Overload analytics service.
 */
public final class TankAmmoQueries {

    private static final String CRLF = "\r\n";
    private static final String TAG_GET_AMMO = " GET\n";
    private static final String TAG_PUT_AMMO = " PUT\n";
    private static final String WEIGHT_BOUNDS_ERROR_MSG = "Second argument isn't valid "
                                                     + "(should be a number in range between 0 and 1";
    private static final String WEIGHT_MISSING_ERROR_MSG = "Given two arguments instead of 3 expected\n"
                                                         + "Retry completing them with specific weight argument";
    private static final int KEY_LENGTH = 256;

    /**
     * class instance const.
     */
    private TankAmmoQueries() {
        // Not supposed to be instantiated
    }

    /**
     * retrieves quasi-random values to be pushed to the store by PUT requests.
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
     * @param numOfKeys - number of keys to be initially provided for pushing into the storage via PUT requests
     */
    private static void putRandKey(final long numOfKeys) throws IOException {
        for (long i = 0; i < numOfKeys; i++) {
            putKey(Long.toString(i));
        }
    }

    /**
     * generates PUT requests featured to resolving partial overwrites among keys stored (query option #2).
     *
     * @param numOfKeys - number of keys to be initially provided for pushing into the storage via PUT requests
     * @param overwritesWeight - specific weight of keys resolved to be potentially modified
     *                         throughout each test session
     */
    private static void putWhenOverwriting(final long numOfKeys, final double overwritesWeight) throws IOException {

        if (overwritesWeight < 0 || overwritesWeight > 1) {
            throw new IllegalArgumentException(WEIGHT_BOUNDS_ERROR_MSG);
        }

        long initKey = 0;

        final int boundKey = (int) (overwritesWeight * 100);
        int nextKey;
        for (long i = 0; i < numOfKeys; i++) {
            nextKey = ThreadLocalRandom.current().nextInt(100);
            if (nextKey <= boundKey) {
                final String key = Long.toString(ThreadLocalRandom.current().nextLong(initKey));
                putKey(key);
            } else {
                putKey(Long.toString(initKey));
                initKey++;
            }
        }
    }

    /**
     * generates GET requests consistently with continuous distribution law (query option #3).
     *
     * @param numOfKeys - number of keys to be initially provided for fetching from the storage via GET requests
     */
    private static void getKeyContinuously(final int numOfKeys) throws IOException {
        for (long i = 0; i < numOfKeys; i++) {
            final int key = new Random().nextInt(numOfKeys);
            getKey(Long.toString(key));
        }
    }

    /**
     * generates GET requests assumed to apply test reading of data in the way it follows flash tendency
     * to handle temporarily rising number of requests for the newest added / refreshed contents (query option #4).
     *
     * @param numOfKeys - number of keys to be available for fetching from the storage via GET requests
     * @param newestEntryShare - specific weight of entries assumed as those recently pushed into the store
     */
    private static void getLatestKey(final long numOfKeys, final double newestEntryShare) throws IOException {

        if (newestEntryShare < 0 || newestEntryShare > 1) {
            throw new IllegalArgumentException(WEIGHT_BOUNDS_ERROR_MSG);
        }

        final Random rand = new Random();
        long key;

        for (long i = 0; i < numOfKeys; i++) {
            key = (int) Math.round(rand.nextGaussian() * numOfKeys * newestEntryShare  + numOfKeys * (1 - newestEntryShare));
            if (key >= numOfKeys) {
                key = numOfKeys - 1;
            } else if (key < 0) {
                key = 0;
            }
            getKey(Long.toString(key));
        }
    }

    /**
     * generates mixed (PUT has a share equal one of GET, i.e. 50% precisely) requests consistently
     * with continuous distribution law (query option #5).
     *
     * @param numOfKeys - number of keys to be initially provided for pushing into / fetching from
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

        try (Writer writer = new OutputStreamWriter(outStream, StandardCharsets.US_ASCII)) {
            writer.write("GET /v0/entity?id=" + keyStr + " HTTP/1.1" + CRLF);
            writer.write(CRLF);
        }
        outStream.write(Integer.toString(outStream.size()).getBytes(StandardCharsets.US_ASCII));
        outStream.write(TAG_GET_AMMO.getBytes(StandardCharsets.US_ASCII));
        outStream.writeTo(System.out);
        outStream.write(CRLF.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * issues common request by any PUT testing option determined before running any supported test .
     *
     * @param keyStr - key written into a String given
     */
    private static void putKey(final String keyStr) throws IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final byte[] value = getRand();

        try (Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8)) {
            writer.write("PUT /v0/entity?id=" + keyStr + " HTTP/1.1" + CRLF);
            writer.write("Content-Length: " + value.length + CRLF);
            writer.write(CRLF);
        }
        outStream.write(value);
        outStream.write(Integer.toString(outStream.size()).getBytes(StandardCharsets.UTF_8));
        outStream.write(TAG_PUT_AMMO.getBytes(StandardCharsets.UTF_8));
        outStream.writeTo(System.out);
        outStream.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * launches picking test option to generate tank ammo respectively.
     *
     * @param args - cmd arguments to execute generating ammo upon test pick
     */
    public static void main(final String [] args) throws IOException {

        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Given inconsistent number of arguments before program running");
        }

        final String querySelected = args[0];
        final int requestPool = Integer.parseInt(args[1]);

        switch (querySelected) {
            case "1":
                putRandKey(requestPool);
                break;
            case "2":
                try {
                    final double opt2Weight = Double.parseDouble(args[2]);
                    putWhenOverwriting(requestPool, opt2Weight);
                } catch (ArrayIndexOutOfBoundsException exc) {
                    System.out.println(WEIGHT_MISSING_ERROR_MSG);
                }
                break;
            case "3":
                getKeyContinuously(requestPool);
                break;
            case "4":
                try {
                    final double opt4Weight = Double.parseDouble(args[2]);
                    getLatestKey(requestPool, opt4Weight);
                } catch (ArrayIndexOutOfBoundsException exc) {
                    System.out.println(WEIGHT_MISSING_ERROR_MSG);
                }
                break;
            case "5":
                mixPutGetLoad(requestPool);
                break;
            default:
                throw new UnsupportedOperationException("Option " + querySelected + " not supported yet");
        }
    }
}
