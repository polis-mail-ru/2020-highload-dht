package ru.mail.polis.util;

import java.io.IOException;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws IOException;

    /**
     * Execute and return 1 if no exception thrown and check is false and 0 otherwise.
     * @param check boolean checker
     * @return 1 on success and 0 on failure
     */
    default int returnPlusCountIfNoException(final boolean check) {
        if (check) {
            try {
                run();
                return 1;
            } catch (IOException e) {
                return 0;
            }
        }
        return 0;
    }
}
