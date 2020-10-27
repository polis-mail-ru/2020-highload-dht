package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ResponseAnalyzer<V> {

    final protected Lock innerLock;
    final protected Lock outerLock;
    final protected Condition outerCondition;

    final protected int neededReplicasCount;
    final protected int totalReplicasCount;

    protected int answeredCount;
    protected int failedCount;

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
        return (this.answeredCount + this.failedCount) == this.totalReplicasCount;
    }

    final public void accept(final Response response) {
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

    final public void accept(final V v) {
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

    final public void await(final long l, final TimeUnit timeUnit) throws InterruptedException {
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

    final public Response getResult() {
        innerLock.lock();
        try {
            return privateGetResult();
        } finally {
            innerLock.unlock();
        }
    }

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
