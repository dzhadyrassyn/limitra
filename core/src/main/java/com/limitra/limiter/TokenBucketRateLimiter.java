package com.limitra.limiter;

import com.limitra.time.TimeProvider;
import java.util.Objects;

public class TokenBucketRateLimiter implements RateLimiter {

    private final TimeProvider timeProvider;
    private final long capacity;
    private final double refillRatePerSecond;
    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(TimeProvider time, long capacity, double refillRatePerSecond) {
        Objects.requireNonNull(time);
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (refillRatePerSecond < 0) {
            throw new IllegalArgumentException("RefillRatePerSecond must not be negative");
        }
        this.timeProvider = time;
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = capacity;
        this.lastRefillNanos = time.nowNanos();
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public synchronized boolean tryAcquire(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("Permits must be positive");
        }

        if (permits > capacity) {
            return false;
        }

        if (refillRatePerSecond == 0 && availableTokens < permits) {
            return false;
        }

        long now = timeProvider.nowNanos();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos > 0) {
            double elapsedSeconds = (double) elapsedNanos / 1_000_000_000;
            availableTokens =
                    Math.min(capacity, availableTokens + elapsedSeconds * refillRatePerSecond);
        }
        lastRefillNanos = now;

        if (availableTokens >= permits) {
            availableTokens -= permits;
            if (availableTokens < 0d) availableTokens = 0d;
            return true;
        }

        return false;
    }
}
