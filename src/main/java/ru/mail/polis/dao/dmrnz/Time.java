package ru.mail.polis.dao.dmrnz;

final class Time {
    private static int countNanos;
    private static long lastTimeInMillis;

    private Time() {
    }

    /**
     * Checks current time in millis and counts nanoseconds.
     *
     * @return Returns current time in nano seconds
     */
    static long currentTimeInNano() {
        final long currentTimeMillis = System.currentTimeMillis();
        if (lastTimeInMillis != currentTimeMillis) {
            lastTimeInMillis = currentTimeMillis;
            countNanos = 0;
        }
        return lastTimeInMillis * 1_000_000 + countNanos++;
    }
}
