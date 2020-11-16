package ru.mail.polis.service.ivanovandrey;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

    public class AmmoGenerator {
        private static final int VALUE_LENGTH = 256;
        private static final String ERRMSG = "Usage:\n\tjava -cp build/classes/java/main"
                + " ru.mail.polis.service.<login>."
                + "AmmoGenerator <put|get> <requests>";
        private static final String RN = "\r\n";
        private static Random random = new Random();

        /**
         * Empty constructor.
         * For the honor of the Codeclimate.
         */
        private AmmoGenerator(){
        }

        @NotNull
        private static byte[] randomValue() {
            final byte[] result = new byte[VALUE_LENGTH];
            ThreadLocalRandom.current().nextBytes(result);
            return result;
        }

        /**
         * Create put request.
         * @param out - file.
         * @param key - key.
         * @param value - value.
         */
        private static void put(@NotNull final OutputStream out,
                                @NotNull final String key,
                                @NotNull final byte[] value) throws IOException {
            final ByteArrayOutputStream request = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(request, StandardCharsets.US_ASCII)) {
                writer.write("PUT /v0/entity?id=" + key + " HTTP/1.1\r\n");
                writer.write("Content-Length: " + value.length + RN);
                writer.write(RN);
            }
            request.write(value);
            out.write(Integer.toString(request.size()).getBytes(StandardCharsets.US_ASCII));
            out.write(" put\n".getBytes(StandardCharsets.US_ASCII));
            request.writeTo(out);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }

        /**
         * Create get request.
         * @param out - file.
         * @param key - key.
         */
        public static void get(@NotNull final OutputStream out,
                               @NotNull final String key) throws IOException {
            final ByteArrayOutputStream request = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(request, StandardCharsets.US_ASCII)) {
                writer.write("GET /v0/entity?id=" + key + " HTTP/1.1\r\n");
                writer.write(RN);
            }
            out.write(Integer.toString(request.size()).getBytes(StandardCharsets.US_ASCII));
            out.write(" get\n".getBytes(StandardCharsets.US_ASCII));
            request.writeTo(out);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }

        private static void one(final int requests,
                                @NotNull final FileOutputStream file) throws IOException {
            for (int i = 0; i < requests; i++) {
                put(file,String.valueOf(i), randomValue());
            }
        }

        private static void two(final int requests,
                                @NotNull final FileOutputStream file) throws IOException {
            for (int i = 0; i < requests; i++) {
                if (i % 10 == 0 && i != 0) {
                    final long owerwrite = random.nextInt(i);
                    put(file, String.valueOf(owerwrite), randomValue());
                } else {
                    put(file, String.valueOf(i), randomValue());
                }
            }
        }

        private static void three(final int requests,
                                  @NotNull final FileOutputStream file) throws IOException {
            for (int i = 0; i < requests; i++) {
                final int key = random.nextInt(requests);
                get(file, String.valueOf(key));
            }
        }

        private static void four(final int requests,
                                 @NotNull final FileOutputStream file) throws IOException {
            for (int i = 0; i < requests; i++) {
                if (i % 10 == 0) {
                    final int key = random.nextInt(requests - requests / 10);
                    get(file, String.valueOf(key));
                } else {
                    final int key = random.nextInt(requests / 10) + (requests * 9) / 10;
                    get(file, String.valueOf(key));
                }
            }
        }

        private static void five(final int requests,
                                 @NotNull final FileOutputStream file) throws IOException {
            int existingKey = 0;
            put(file, String.valueOf(existingKey), randomValue());
            existingKey++;
            for (int i = 1; i < requests; i++) {
                final boolean choice = random.nextBoolean();
                if (choice) {
                    final int key = random.nextInt(existingKey);
                    get(file, String.valueOf(key));
                } else {
                    put(file, String.valueOf(existingKey), randomValue());
                    existingKey++;
                }
            }
        }

        public static void main(final String[] args) throws IOException {
            if (args.length != 3) {
                System.err.println(ERRMSG);
                System.exit(-1);
            }

            final String mode = args[0];
            final int requests = Integer.parseInt(args[1]);
            try (FileOutputStream file = new FileOutputStream(args[2])) {
                switch (mode) {
                    case "one":
                        one(requests, file);
                        break;
                    case "two":
                        two(requests, file);
                        break;
                    case "three":
                        three(requests, file);
                        break;
                    case "four":
                        four(requests, file);
                        break;
                    case "five":
                        five(requests, file);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported mode: " + mode);
                }
            }
        }
    }

