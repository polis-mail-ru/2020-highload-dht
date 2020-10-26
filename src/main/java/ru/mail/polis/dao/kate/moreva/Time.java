package ru.mail.polis.dao.kate.moreva;

public class Time {
    private static int count;
    private static long lastTime;

    private Time () {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Method returns current time in nano seconds.
     */
    static long currentTime() {
        synchronized (Time.class) {
            final var time = System.currentTimeMillis();
            if (lastTime != time) {
                lastTime = time;
                count = 0;
            }
            return lastTime * 1_000_000 + count++;
        }
    }

}
