package ru.mail.polis.service.stasyanoi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomExecutor extends ThreadPoolExecutor {

    public CustomExecutor(final int corePoolSize,
                          final int maximumPoolSize,
                          final long keepAliveTime,
                          final TimeUnit unit,
                          final BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Get custom executor.
     *
     * @return - custom executor.
     */
    public static CustomExecutor getExecutor() {
        final int nThreads = Runtime.getRuntime().availableProcessors();
        return new CustomExecutor(nThreads, nThreads,0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(Constants.TASK_THRESHOLD));

    }
}
