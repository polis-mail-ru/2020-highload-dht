package ru.mail.polis.service.stasyanoi;

import one.nio.http.HttpSession;
import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomExecutor extends ThreadPoolExecutor {
    
    private final static Logger logger = LoggerFactory.getLogger(CustomExecutor.class);

    public CustomExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public void setSessionForRejectedError(HttpSession errorSession) {
        this.setRejectedExecutionHandler((r, executor) -> send503Error(errorSession));
    }

    public static CustomExecutor getExecutor() {
        int nThreads = Runtime.getRuntime().availableProcessors();

        return new CustomExecutor(nThreads, nThreads,0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(20));

    }

    private void send503Error(HttpSession errorSession) {
        try {
            errorSession.sendResponse(Util.getResponseWithNoBody(Response.SERVICE_UNAVAILABLE));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
