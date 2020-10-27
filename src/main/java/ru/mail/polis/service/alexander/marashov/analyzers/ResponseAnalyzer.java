package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ResponseAnalyzer<V> {

    protected final Lock innerLock;
    protected final Lock outerLock;
    protected final Condition outerCondition;

    protected final int neededReplicasCount;
    protected final int totalReplicasCount;

    protected int answeredCount;
    protected int failedCount;

    /**
     * Abstract response analyzer that accumulates responses from DAO's methods and analyzes them.
     *
     * @param neededReplicasCount - how many replicas is required.
     * @param totalReplicasCount  - how many replicas is expected.
     */
    public ResponseAnalyzer(final int neededReplicasCount, final int totalReplicasCount) {
        assert 0 < neededReplicasCount;

        this.neededReplicasCount = neededReplicasCount;
        this.totalReplicasCount = totalReplicasCount;

        this.innerLock = new ReentrantLock();
        this.outerLock = new ReentrantLock();
        this.outerCondition = outerLock.newCondition();

        this.answeredCount = 0;
        this.failedCount = 0;
    }

    protected boolean hasEnoughAnswers() {
        return this.answeredCount + this.failedCount == this.totalReplicasCount;
    }

    /**
     * Accept the next response to analyze.
     * @param response - the analyzed response.
     */
    public final void accept(final Response response) {
        innerLock.lock();
        try {
            privateAccept(response);
            if (hasEnoughAnswers()) {
                signalAll();
            }
        } finally {
            innerLock.unlock();
        }
    }

    /**
     * Accept the next value to analyze.
     * @param v - the analyzed value.
     */
    public final void accept(final V v) {
        innerLock.lock();
        try {
            privateAccept(v);
            if (hasEnoughAnswers()) {
                signalAll();
            }
        } finally {
            innerLock.unlock();
        }
    }

    /**
     *
     * @param l - number of time units.
     * @param timeUnit - units for measuring time.
     * @throws InterruptedException if the wait is interrupted.
     */
    public final void await(final long l, final TimeUnit timeUnit) throws InterruptedException {
        outerLock.lock();
        try {
            while (!hasEnoughAnswers()) {
                final boolean timeIsOut = !outerCondition.await(l, timeUnit);
                if (timeIsOut) {
                    break;
                }
            }
        } finally {
            outerLock.unlock();
        }
    }

    /**
     * Analyze received data and get the correct result.
     * @return correct response to send.
     */
    public final Response getResult() {
        innerLock.lock();
        try {
            return privateGetResult();
        } finally {
            innerLock.unlock();
        }
    }

    /**
     * Send a signal to everyone waiting for the results to be processed.
     */
    protected void signalAll() {
        outerLock.lock();
        try {
            outerCondition.signalAll();
        } finally {
            outerLock.unlock();
        }
    }

    protected abstract void privateAccept(final Response response);

    protected abstract void privateAccept(final V v);

    protected abstract Response privateGetResult();
}
