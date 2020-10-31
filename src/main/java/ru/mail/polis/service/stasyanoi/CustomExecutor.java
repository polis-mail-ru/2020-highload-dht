package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomExecutor extends ThreadPoolExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomExecutor.class);

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
                new LinkedBlockingQueue<>(100));

    }

    private void send503Error(final HttpSession errorSession) {
        try {
            errorSession.sendResponse(Util.getResponseWithNoBody(Response.SERVICE_UNAVAILABLE));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
