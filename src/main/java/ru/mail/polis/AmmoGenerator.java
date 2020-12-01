package ru.mail.polis;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class AmmoGenerator {
    private static final String CRLF = "\r\n";
    private static final String PUT_URL = "PUT /v0/entity?id=";
    private static final String GET_URL = "GET /v0/entity?id=";
    private static final byte[] PUT_BYTES = " put\n".getBytes(US_ASCII);
    private static final byte[] GET_BYTES = " get\n".getBytes(US_ASCII);
    private static final String HTTP_VERSION = " HTTP/1.1";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length: ";
    private static Random random = new Random();

    private AmmoGenerator() {
        /* Add private constructor to prevent instantiation */
    }

    /**
     * Generates PUT request based on given params.
     *
     * @param out   - where to write
     * @param key   - request key
     * @param value - request body
     * @throws IOException - something went wrong
     */
    private static void makePutRequest(
            final OutputStream out, final String key, final byte[] value
    ) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, US_ASCII)) {
            writer.write(PUT_URL + key + HTTP_VERSION + CRLF);
            writer.write(CONTENT_LENGTH_HEADER + value.length + CRLF);
            writer.write(CRLF);
        }
        outputStream.write(value);
        out.write(Integer.toString(outputStream.size()).getBytes(US_ASCII));
        out.write(PUT_BYTES);
        outputStream.writeTo(out);
        out.write(CRLF.getBytes(US_ASCII));
    }

    /**
     * Generates GET request based on given params.
     *
     * @param out - where to write
     * @param key - request key
     * @throws IOException - something went wrong
     */
    private static void makeGetRequest(
            final OutputStream out,
            final String key
    ) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, US_ASCII)) {
            writer.write(GET_URL + key + HTTP_VERSION + CRLF);
            writer.write(CRLF);
        }
        out.write(Integer.toString(outputStream.size()).getBytes(US_ASCII));
        out.write(GET_BYTES);
        outputStream.writeTo(out);
        out.write(CRLF.getBytes(US_ASCII));
    }

    /**
     * Generates PUT requests with unique keys.
     *
     * @param numOfRequests - how many requests to generate
     * @param outputFile    - where to write generated requests
     * @throws IOException - something went wrong
     */
    private static void generatePutUnique(
            final int numOfRequests,
            final FileOutputStream outputFile
    ) throws IOException {
        int i = 0;
        while (i < numOfRequests) {
            makePutRequest(outputFile, String.valueOf(i), generateRandomValue());
            i++;
        }
    }

    /**
     * Generates PUT requests partially overwriting existing data.
     *
     * @param numOfRequests - how many requests to generate
     * @param outputFile    - where to write generated requests
     * @throws IOException - something went wrong
     */
    private static void generatePutPartialRewrite(
            final int numOfRequests,
            final FileOutputStream outputFile
    ) throws IOException {
        int i = 0;
        while (i < numOfRequests) {
            if (i % 10 == 0 && i != 0) {
                final long keyToRewrite = random.nextInt(i);
                makePutRequest(outputFile, String.valueOf(keyToRewrite), generateRandomValue());
            } else {
                makePutRequest(outputFile, String.valueOf(i), generateRandomValue());
            }
            i++;
        }
    }

    /**
     * Generates GET requests to existing records using equal distribution.
     *
     * @param numOfRequests - how many requests to generate
     * @param outputFile    - where to write generated requests
     * @throws IOException - something went wrong
     */
    private static void generateGetExisting(
            final int numOfRequests,
            final FileOutputStream outputFile
    ) throws IOException {
        int i = 0;
        while (i < numOfRequests) {
            final int key = random.nextInt(numOfRequests);
            makeGetRequest(outputFile, String.valueOf(key));
            i++;
        }
    }

    /**
     * Generates GET requests to recently created records.
     *
     * @param numOfRequests - how many requests to generate
     * @param outputFile    - where to write generated requests
     * @throws IOException - something went wrong
     */
    private static void generateGetExistingWithOffset(
            final int numOfRequests, final FileOutputStream outputFile
    ) throws IOException {
        int i = 0;
        while (i < numOfRequests) {
            final double randomGaussKey = random.nextGaussian() * numOfRequests * 0.1 + numOfRequests * 0.9;
            int currentKey = (int) Math.round(randomGaussKey);
            if (currentKey >= numOfRequests) {
                currentKey = numOfRequests - 1;
            } else if (currentKey < 0) {
                currentKey = 0;
            }
            makeGetRequest(outputFile, String.valueOf(currentKey));
            i++;
        }
    }

    /**
     * Generates mixture of PUT & GET requests.
     *
     * @param numOfRequests - how many requests to generate
     * @param outputFile    - where to write generated requests
     * @throws IOException - something went wrong
     */
    private static void generatePutGetMixed(
            final int numOfRequests,
            final FileOutputStream outputFile
    ) throws IOException {
        int start = 0;
        makePutRequest(outputFile, String.valueOf(start), generateRandomValue());
        start++;

        int i = 1;
        while (i < numOfRequests) {
            final boolean shouldGenerateGet = random.nextBoolean();
            if (shouldGenerateGet) {
                final int key = random.nextInt(start);
                makeGetRequest(outputFile, String.valueOf(key));
            } else {
                makePutRequest(outputFile, String.valueOf(start), generateRandomValue());
                start++;
            }
            i++;
        }
    }

    /**
     * This generates PUT or GET Ammo requests based on a user choice.
     *
     * @param args - request settings array
     * @throws IOException - something went wrong
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid generator arguments. Input format should be <mode> <numOfRequests> <outputFile>"
            );
        }

        final String mode = args[0];
        final int numOfRequests = Integer.parseInt(args[1]);
        try (FileOutputStream outputFile = new FileOutputStream(args[2])) {
            switch (mode) {
                case "PUT_UNIQUE":
                    generatePutUnique(numOfRequests, outputFile);
                    break;
                case "PUT_PARTIAL_REWRITE":
                    generatePutPartialRewrite(numOfRequests, outputFile);
                    break;
                case "GET_EXISTING":
                    generateGetExisting(numOfRequests, outputFile);
                    break;
                case "GET_EXISTING_WITH_OFFSET":
                    generateGetExistingWithOffset(numOfRequests, outputFile);
                    break;
                case "PUT_GET_MIXED":
                    generatePutGetMixed(numOfRequests, outputFile);
                    break;
                default:
                    throw new IllegalArgumentException("Mode " + mode + " is not allowed");
            }
        }
    }

    private static byte[] generateRandomValue() {
        final byte[] result = new byte[256];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }
}
