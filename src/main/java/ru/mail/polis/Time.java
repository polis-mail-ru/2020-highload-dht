package ru.mail.polis;

public final class Time {

    private static int count;
    private static long prevTime;

    private Time() {
    }

    /**
     * Метод, возвращающий текущее время в nano seconds.
     *
     * @return prevTime
     */
    public static long getCurrentTime() {
        final long currentTimeMillis = System.currentTimeMillis();
        if (prevTime != currentTimeMillis) {
            prevTime = currentTimeMillis;
            count = 0;
        }
        return prevTime * 1_000_000 + count++;
    }
}
