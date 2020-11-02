package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ResponseAnalyzer<V> {

    protected final Lock lock;
    protected final Condition condition;

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

        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();

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
        lock.lock();
        try {
            privateAccept(response);
            if (hasEnoughAnswers()) {
                signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Accept the next value to analyze.
     * @param v - the analyzed value.
     */
    public final void accept(final V v) {
        lock.lock();
        try {
            privateAccept(v);
            if (hasEnoughAnswers()) {
                signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for condition signal or timeout.
     * @param l - number of time units.
     * @param timeUnit - units for measuring time.
     * @throws InterruptedException if the wait is interrupted.
     */
    public final void await(final long l, final TimeUnit timeUnit) throws InterruptedException {
        lock.lock();
        try {
            while (!hasEnoughAnswers()) {
                final boolean timeIsOut = !condition.await(l, timeUnit);
                if (timeIsOut) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Analyze received data and get the correct result.
     * @return correct response to send.
     */
    public final Response getResult() {
        lock.lock();
        try {
            return privateGetResult();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Send a signal to everyone waiting for the results to be processed.
     */
    protected void signalAll() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    protected abstract void privateAccept(final Response response);

    protected abstract void privateAccept(final V v);

    protected abstract Response privateGetResult();
}
