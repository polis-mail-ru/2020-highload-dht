package ru.mail.polis.service.stasyanoi;

import java.nio.charset.StandardCharsets;

public final class Constants {

    public static final String TIMESTAMP_HEADER_NAME = "Time: ";
    public static final String TRANSFER_ENCODING_HEADER_NAME = "Transfer-Encoding: ";
    public static final String CONNECTION_HEADER_NAME = "Connection: ";
    public static final int EMPTY_BODY_SIZE = 9;
    public static final int TASK_THRESHOLD = 100;
    public static final String TRUE = "true";
    public static final String REPLICAS = "replicas";
    public static final String SHOULD_REPLICATE = "reps";
    public static final int HASH_THRESHOLD = 30000;
    public static final int TIMESTAMP_LENGTH = 8;
    public static final byte[] EOF = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] EOL = "\n".getBytes(StandardCharsets.US_ASCII);

    private Constants() {

    }
}
