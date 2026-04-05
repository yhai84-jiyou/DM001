package com.helpmanual.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;
        if (System.currentTimeMillis() - info.firstAttemptTime > BLOCK_DURATION_MS) {
            attempts.remove(key);
            return false;
        }
        return info.count.get() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, v) -> {
            if (v == null || System.currentTimeMillis() - v.firstAttemptTime > BLOCK_DURATION_MS) {
                return new AttemptInfo();
            }
            v.count.incrementAndGet();
            return v;
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private static class AttemptInfo {
        final AtomicInteger count = new AtomicInteger(1);
        final long firstAttemptTime = System.currentTimeMillis();
    }
}
