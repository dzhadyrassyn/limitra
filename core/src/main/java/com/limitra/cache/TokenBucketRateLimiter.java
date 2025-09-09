package com.limitra.cache;

public class TokenBucketRateLimiter implements RateLimiter {

    private final TimeProvider timeProvider;
    private final long capacity;
    private final double refillRatePerSecond;

    public TokenBucketRateLimiter(TimeProvider time, long capacity, double permitsPerSecond) {
        this.timeProvider = time;
        this.capacity = capacity;
        this.refillRatePerSecond = permitsPerSecond;
    }

    @Override
    public boolean tryAcquire() {
        return false;
    }

    @Override
    public boolean tryAcquire(int permits) {
        return false;
    }
}
