package ru.mail.polis.dao.boriskin;

import java.util.concurrent.TimeUnit;

final class Utils {
    private static long time;
    // обогащение
    private static int counter;

    private Utils() {
        // do nothing
    }

    static long getTime() {
        final long currentTime = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
        if (currentTime != time) {
            time = currentTime;
            counter = 0;
        }
        return currentTime + counter++;
    }
}
