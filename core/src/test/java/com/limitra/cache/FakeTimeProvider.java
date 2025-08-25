package com.limitra.cache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test-only, monotonic, relative time. Negative advances are rejected.
 */
public class FakeTimeProvider implements TimeProvider {

    private final AtomicLong nanos;

    public FakeTimeProvider() {
        this.nanos = new AtomicLong();
    }

    public FakeTimeProvider(long nanos) {
        if (nanos < 0) {
            throw new IllegalArgumentException("nanos must be >= 0");
        }
        this.nanos = new AtomicLong(nanos);
    }

    public long nowNanos() {
        return nanos.get();
    }

    public void advanceNanos(long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("delta must be >= 0");
        }
        nanos.addAndGet(delta);
    }

    public void advanceMillis(long ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("ms must be >= 0");
        }
        advanceNanos(TimeUnit.MILLISECONDS.toNanos(ms));
    }

    public void advanceSeconds(long s) {
        if (s < 0) {
            throw new IllegalArgumentException("s must be >= 0");
        }
        advanceNanos(TimeUnit.SECONDS.toNanos(s));
    }
}
