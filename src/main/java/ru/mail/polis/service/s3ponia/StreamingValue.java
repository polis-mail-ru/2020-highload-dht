package ru.mail.polis.service.s3ponia;

public interface StreamingValue {

    /**
     * Produces chunk for http streaming.
     * For example, if payload = "123", returns byte[] = {'3', '\r', '\n', '1', '2', '3', '\r', '\n'}.
     * @return a {@code byte[]}
     */
    byte[] value();
}
