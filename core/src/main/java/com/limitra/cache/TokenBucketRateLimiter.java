package com.limitra.cache;

public class TokenBucketRateLimiter implements RateLimiter {

    public TokenBucketRateLimiter(TimeProvider time, long capacity, double permitsPerSecond) {}

    @Override
    public boolean tryAcquire() {
        return false;
    }

    @Override
    public boolean tryAcquire(int permits) {
        return false;
    }
}
