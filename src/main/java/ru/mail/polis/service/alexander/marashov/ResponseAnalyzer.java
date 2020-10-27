package ru.mail.polis.service.alexander.marashov;

import one.nio.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.alexander.marashov.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ResponseAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ResponseAnalyzer.class);

    final private Map<Value, Integer> answersMap;
    final private int neededReplicasCount;

//    private Value newestValue;

    public int answeredCount;
    private Value tombstoneValue;

    private Lock lock = new ReentrantLock();

    public ResponseAnalyzer(final int neededReplicasCount) {
        assert 0 < neededReplicasCount;

        this.answersMap = new HashMap<>();
        this.neededReplicasCount = neededReplicasCount;
        this.tombstoneValue = null;
        this.answeredCount = 0;
    }

    public boolean hasEnoughAnswers() {
        return this.answeredCount >= neededReplicasCount;
    }

    public boolean accept(final Response response) {
        lock.lock();
        try {
            if (response == null) {
                return hasEnoughAnswers();
            }
            if (response.getStatus() == 404) {
                answeredCount++;
            }
            if (response.getStatus() != 200) {
                return hasEnoughAnswers();
            }

            final Value value;
            try {
                value = ValueSerializer.deserialize(response.getBody());
            } catch (final ClassNotFoundException | IOException e) {
                log.error("Value deserialize error", e);
                return hasEnoughAnswers();
            }
            return accept(value);
        } finally {
            lock.unlock();
        }
    }

    public boolean accept(final Value value) {
        lock.lock();
        try {
            answeredCount++;
            if (value != null) {
                if (value.isTombstone()) {
                    tombstoneValue = value;
                    return true;
                }
                answersMap.compute(value, (v, oldCount) -> {
                    if (oldCount == null) {
                        return 1;
                    }
                    return oldCount + 1;
                });
            }

            return hasEnoughAnswers();
        } finally {
            lock.unlock();
        }
    }

    public Value getCorrectValue() {
        lock.lock();
        try {
            if (tombstoneValue != null) {
                return tombstoneValue;
            }
            if (!hasEnoughAnswers()) {
                return null;
            }
            if (answersMap.isEmpty()) {
                return new Value(0L, null);
            }
            Value correctValue = null;
            int maxCount = 0;
            for (final Map.Entry<Value, Integer> answerEntry : answersMap.entrySet()) {
                final Value value = answerEntry.getKey();
                final int count = answerEntry.getValue();
                if (
                        correctValue == null
                                || count > maxCount
                                || (count == maxCount && value.compareTo(correctValue) > 0)
                ) {
                    correctValue = value;
                }
            }
            return correctValue;
        } finally {
            lock.unlock();
        }
    }
}
