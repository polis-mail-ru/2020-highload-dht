package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpSession;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.mail.polis.service.stasyanoi.Util.send503Error;

public class CustomExecutor extends ThreadPoolExecutor {
    
    public CustomExecutor(final int corePoolSize,
                          final int maximumPoolSize,
                          final long keepAliveTime,
                          final TimeUnit unit,
                          final BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Set session for error sending.
     *
     * @param errorSession - session to which to send the error.
     */
    public void setSessionForRejectedError(final HttpSession errorSession) {
        this.setRejectedExecutionHandler((r, executor) -> send503Error(errorSession));
    }

    /**
     * Get custom executor.
     *
     * @return - custom executor.
     */
    public static CustomExecutor getExecutor() {
        final int nThreads = Runtime.getRuntime().availableProcessors();
        return new CustomExecutor(nThreads, nThreads,0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(20));

    }
}
