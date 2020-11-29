package ru.mail.polis.expire.service;

import java.time.Duration;

public class TwoNodeExpireTest extends ClusterExpireTestBase {
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    @Override
    int getClusterSize() {
        return 2;
    }
}
