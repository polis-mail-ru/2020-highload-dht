package ru.mail.polis.service.zvladn7;

import java.io.IOException;

/**
 * Implement this interface for processing http requests.
 */
@FunctionalInterface
public interface Processor {

    /**
     * Process http requests.
     * @throws IOException - error sending response
     */
    void process() throws IOException;

}
